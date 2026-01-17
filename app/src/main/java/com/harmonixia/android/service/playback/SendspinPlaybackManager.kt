package com.harmonixia.android.service.playback

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.media3.exoplayer.ExoPlayer
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.util.Logger
import java.net.URI
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class SendspinPlaybackManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val settingsDataStore: SettingsDataStore,
    equalizerManager: EqualizerManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val audioOutput = SendspinAudioOutput(equalizerManager, ioDispatcher)
    private val timeSyncState = SendspinTimeSync()

    private var player: ExoPlayer? = null
    private var settingsJob: Job? = null
    private var reconnectJob: Job? = null
    private var handshakeTimeoutJob: Job? = null
    private var timeSyncJob: Job? = null
    private var stateJob: Job? = null

    private var serverUrl: String = ""
    private var authToken: String = ""
    private var clientId: String = ""
    private var webSocket: WebSocket? = null
    private var connecting = false
    private var handshakeComplete = false
    private var manualStop = false
    private var reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
    private var authFailureToken: String? = null
    private var streamActive = false
    private var lastServerMessageAtMs = 0L

    private var volume: Float = 1f
    private var muted: Boolean = false
    private var lastVolume: Float = 1f
    private var suppressPlayerVolumeUpdate = false

    fun attachPlayer(player: ExoPlayer) {
        this.player = player
        val initialVolume = player.volume.coerceIn(0f, 1f)
        volume = initialVolume
        lastVolume = initialVolume
    }

    fun start() {
        if (settingsJob != null) return
        manualStop = false
        settingsJob = scope.launch {
            combine(
                settingsDataStore.getServerUrl(),
                settingsDataStore.getAuthToken(),
                settingsDataStore.getSendspinClientId()
            ) { url, token, id -> Triple(url.trim(), token.trim(), id.trim()) }
                .distinctUntilChanged()
                .collect { (url, token, id) ->
                    serverUrl = url
                    authToken = token
                    if (authFailureToken != null && authFailureToken != authToken) {
                        authFailureToken = null
                    }
                    if (id.isBlank()) {
                        val newId = generateClientId()
                        settingsDataStore.saveSendspinClientId(newId)
                        clientId = newId
                    } else {
                        clientId = id
                    }
                    if (serverUrl.isBlank()) {
                        closeWebSocket("Sendspin disabled")
                        return@collect
                    }
                    reconnect()
                }
        }
    }

    fun stop() {
        manualStop = true
        settingsJob?.cancel()
        settingsJob = null
        cancelHandshakeJobs()
        closeWebSocket("Sendspin stop")
        audioOutput.release()
        scope.cancel()
    }

    fun onPlayerVolumeChanged(newVolume: Float) {
        if (suppressPlayerVolumeUpdate) return
        val normalized = newVolume.coerceIn(0f, 1f)
        val wasMuted = muted
        if (wasMuted && normalized > 0f) {
            muted = false
        }
        if (abs(volume - normalized) < VOLUME_EPSILON && wasMuted == muted) return
        volume = normalized
        lastVolume = normalized
        applyVolume()
        sendPlayerState()
    }

    private fun reconnect() {
        if (manualStop) return
        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
        closeWebSocket("Reconnect")
        connect()
    }

    private fun connect() {
        if (connecting || manualStop) return
        if (serverUrl.isBlank() || clientId.isBlank()) return
        if (authFailureToken != null && authFailureToken == authToken) return
        connecting = true
        handshakeComplete = false
        val wsUrl = buildSendspinUrl(serverUrl, authToken.isNotBlank())
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, webSocketListener)
        scheduleHandshakeTimeout()
    }

    private fun scheduleHandshakeTimeout() {
        handshakeTimeoutJob?.cancel()
        handshakeTimeoutJob = scope.launch {
            delay(HANDSHAKE_TIMEOUT_MS)
            if (!handshakeComplete && !manualStop) {
                Logger.w(TAG, "Sendspin handshake timed out")
                closeWebSocket("Handshake timeout")
            }
        }
    }

    private fun cancelHandshakeJobs() {
        handshakeTimeoutJob?.cancel()
        handshakeTimeoutJob = null
        timeSyncJob?.cancel()
        timeSyncJob = null
        stateJob?.cancel()
        stateJob = null
    }

    private fun closeWebSocket(reason: String) {
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(NORMAL_CLOSE_CODE, reason)
        webSocket = null
        connecting = false
        handshakeComplete = false
        streamActive = false
        lastServerMessageAtMs = 0L
        cancelHandshakeJobs()
        audioOutput.stop()
    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Logger.i(TAG, "Sendspin socket opened")
            connecting = false
            if (authToken.isNotBlank()) {
                sendAuth()
            } else {
                sendClientHello()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleJsonMessage(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            handleBinaryMessage(bytes)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Logger.w(TAG, "Sendspin socket failure", t)
            handleDisconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Logger.i(TAG, "Sendspin socket closed: $code $reason")
            handleDisconnect()
        }
    }

    private fun handleDisconnect() {
        closeWebSocket("Disconnected")
        if (manualStop) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelayMs)
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            connect()
        }
    }

    private fun handleJsonMessage(message: String) {
        val root = runCatching { json.parseToJsonElement(message).jsonObject }
            .getOrElse { error ->
                Logger.w(TAG, "Failed to parse Sendspin message", error)
                return
            }
        markServerMessage()
        when (root["type"]?.jsonPrimitive?.contentOrNull) {
            "auth_ok" -> sendClientHello()
            "auth_invalid" -> {
                Logger.w(TAG, "Sendspin auth rejected")
                authFailureToken = authToken
                closeWebSocket("Auth rejected")
            }
            "server/hello" -> handleServerHello()
            "server/time" -> handleServerTime(root)
            "stream/start" -> handleStreamStart(root)
            "stream/end" -> handleStreamEnd()
            "stream/clear" -> handleStreamClear()
            "server/command" -> handleServerCommand(root)
            else -> Unit
        }
    }

    private fun handleBinaryMessage(bytes: ByteString) {
        val payload = bytes.toByteArray()
        if (payload.size < BINARY_HEADER_SIZE) return
        val messageType = payload[0].toInt() and 0xFF
        if (messageType != AUDIO_CHUNK_MESSAGE_TYPE) return
        if (!streamActive) return
        markServerMessage()
        val timestampUs = parseTimestampUs(payload)
        val audioData = payload.copyOfRange(BINARY_HEADER_SIZE, payload.size)
        audioOutput.enqueueAudio(timestampUs, audioData)
    }

    private fun handleServerHello() {
        Logger.i(TAG, "Sendspin handshake complete")
        handshakeComplete = true
        sendPlayerState()
        sendTimeSync()
        startLoops()
    }

    private fun handleServerTime(root: JsonObject) {
        val payload = root["payload"]?.jsonObject ?: return
        val clientTransmitted = payload["client_transmitted"]?.jsonPrimitive?.longOrNull ?: return
        val serverReceived = payload["server_received"]?.jsonPrimitive?.longOrNull ?: return
        val serverTransmitted = payload["server_transmitted"]?.jsonPrimitive?.longOrNull ?: return
        val nowUs = nowUs()
        val delay = ((nowUs - clientTransmitted) - (serverTransmitted - serverReceived)) / 2
        timeSyncState.update(abs(delay))
    }

    private fun handleStreamStart(root: JsonObject) {
        val payload = root["payload"]?.jsonObject ?: return
        val playerInfo = payload["player"]?.jsonObject ?: return
        val codec = playerInfo["codec"]?.jsonPrimitive?.contentOrNull ?: return
        if (codec != "pcm") {
            Logger.w(TAG, "Unsupported Sendspin codec: $codec")
            return
        }
        val sampleRate = playerInfo["sample_rate"]?.jsonPrimitive?.intOrNull ?: return
        val channels = playerInfo["channels"]?.jsonPrimitive?.intOrNull ?: return
        val bitDepth = playerInfo["bit_depth"]?.jsonPrimitive?.intOrNull ?: return
        streamActive = true
        val format = SendspinPcmFormat(sampleRate, channels, bitDepth)
        audioOutput.start(format)
        applyVolume()
    }

    private fun handleStreamEnd() {
        streamActive = false
        audioOutput.stop()
    }

    private fun handleStreamClear() {
        audioOutput.flush()
    }

    private fun handleServerCommand(root: JsonObject) {
        val payload = root["payload"]?.jsonObject ?: return
        val playerPayload = payload["player"]?.jsonObject ?: return
        when (playerPayload["command"]?.jsonPrimitive?.contentOrNull) {
            "volume" -> {
                val volumeValue = playerPayload["volume"]?.jsonPrimitive?.intOrNull ?: return
                setServerVolume(volumeValue)
            }
            "mute" -> {
                val muteValue = playerPayload["mute"]?.jsonPrimitive?.booleanOrNull ?: return
                setServerMuted(muteValue)
            }
        }
    }

    private fun setServerVolume(volumeValue: Int) {
        val normalized = (volumeValue.coerceIn(0, 100) / 100f)
        volume = normalized
        lastVolume = normalized
        muted = false
        applyVolume()
        updatePlayerVolume(normalized)
        sendPlayerState()
    }

    private fun setServerMuted(muteValue: Boolean) {
        muted = muteValue
        val target = if (muteValue) 0f else lastVolume
        applyVolume()
        updatePlayerVolume(target)
        sendPlayerState()
    }

    private fun applyVolume() {
        audioOutput.setVolume(volume, muted)
    }

    private fun updatePlayerVolume(volume: Float) {
        val player = player ?: return
        val normalized = volume.coerceIn(0f, 1f)
        if (abs(player.volume - normalized) < VOLUME_EPSILON) return
        suppressPlayerVolumeUpdate = true
        try {
            player.volume = normalized
        } finally {
            suppressPlayerVolumeUpdate = false
        }
    }

    private fun startLoops() {
        timeSyncJob?.cancel()
        timeSyncJob = scope.launch {
            while (isActive && handshakeComplete) {
                sendTimeSync()
                delay(timeSyncState.intervalMs())
            }
        }
        stateJob?.cancel()
        stateJob = scope.launch {
            while (isActive && handshakeComplete) {
                sendPlayerState()
                delay(STATE_INTERVAL_MS)
            }
        }
    }

    private fun sendAuth() {
        val payload = buildJsonObject {
            put("type", JsonPrimitive("auth"))
            put("token", JsonPrimitive(authToken))
            put("client_id", JsonPrimitive(clientId))
        }
        sendJson(payload)
    }

    private fun sendClientHello() {
        val payload = buildJsonObject {
            put("type", JsonPrimitive("client/hello"))
            put(
                "payload",
                buildJsonObject {
                    put("client_id", JsonPrimitive(clientId))
                    put("name", JsonPrimitive(buildClientName()))
                    put("version", JsonPrimitive(1))
                    put("supported_roles", buildSupportedRoles())
                    put(
                        "device_info",
                        buildJsonObject {
                            put("product_name", JsonPrimitive(Build.MODEL ?: "Android"))
                            put("manufacturer", JsonPrimitive(Build.MANUFACTURER ?: "Android"))
                            put("software_version", JsonPrimitive(Build.VERSION.RELEASE ?: ""))
                        }
                    )
                    put(
                        "player_support",
                        buildJsonObject {
                            put("supported_formats", buildSupportedFormats())
                            put("buffer_capacity", JsonPrimitive(BUFFER_CAPACITY_BYTES))
                            put(
                                "supported_commands",
                                buildJsonObjectArray("volume", "mute")
                            )
                        }
                    )
                }
            )
        }
        sendJson(payload)
    }

    private fun sendPlayerState() {
        if (!handshakeComplete) return
        if (isConnectionStale()) {
            Logger.w(TAG, "Sendspin connection stale; reconnecting")
            handleDisconnect()
            return
        }
        val volumePercent = (volume.coerceIn(0f, 1f) * 100f).roundToInt()
        val payload = buildJsonObject {
            put("type", JsonPrimitive("client/state"))
            put(
                "payload",
                buildJsonObject {
                    put(
                        "player",
                        buildJsonObject {
                            put("state", JsonPrimitive("synchronized"))
                            put("volume", JsonPrimitive(volumePercent))
                            put("muted", JsonPrimitive(muted))
                        }
                    )
                }
            )
        }
        sendJson(payload)
    }

    private fun sendTimeSync() {
        if (!handshakeComplete) return
        if (isConnectionStale()) {
            Logger.w(TAG, "Sendspin connection stale; reconnecting")
            handleDisconnect()
            return
        }
        val payload = buildJsonObject {
            put("type", JsonPrimitive("client/time"))
            put(
                "payload",
                buildJsonObject {
                    put("client_transmitted", JsonPrimitive(nowUs()))
                }
            )
        }
        sendJson(payload)
    }

    private fun sendJson(payload: JsonObject) {
        val socket = webSocket ?: return
        val text = json.encodeToString(JsonObject.serializer(), payload)
        if (!socket.send(text)) {
            Logger.w(TAG, "Failed to send Sendspin message")
            handleDisconnect()
        }
    }

    private fun buildSendspinUrl(serverUrl: String, useProxy: Boolean): String {
        val uri = runCatching { URI(serverUrl) }.getOrNull()
        val host = uri?.host ?: serverUrl.replaceFirst("https://", "")
            .replaceFirst("http://", "")
            .substringBefore('/')
        val scheme = when (uri?.scheme) {
            "https" -> "wss"
            "http" -> "ws"
            "wss", "ws" -> uri.scheme
            else -> "ws"
        }
        val port = when {
            useProxy && uri?.port != null && uri.port > 0 -> uri.port
            useProxy -> if (scheme == "wss") 443 else 80
            else -> SENDSPIN_PORT
        }
        return if (useProxy) {
            "$scheme://$host:$port/sendspin"
        } else {
            "$scheme://$host:$SENDSPIN_PORT/sendspin"
        }
    }

    private fun buildClientName(): String {
        val label = context.packageManager.getApplicationLabel(context.applicationInfo).toString()
        val model = Build.MODEL?.trim().orEmpty()
        return if (model.isNotBlank()) "$label ($model)" else label
    }

    private fun buildSupportedRoles(): JsonArray = buildJsonArray {
        add(JsonPrimitive("player@v1"))
    }

    private fun buildSupportedFormats(): JsonArray {
        val formats = listOf(48000, 44100).map { sampleRate ->
            buildJsonObject {
                put("codec", JsonPrimitive("pcm"))
                put("channels", JsonPrimitive(2))
                put("sample_rate", JsonPrimitive(sampleRate))
                put("bit_depth", JsonPrimitive(16))
            }
        }
        return buildJsonArray {
            formats.forEach { add(it) }
        }
    }

    private fun buildJsonObjectArray(vararg values: String): JsonArray = buildJsonArray {
        values.forEach { add(JsonPrimitive(it)) }
    }

    private fun generateClientId(): String {
        return "ma_android_${UUID.randomUUID().toString().replace("-", "").take(10)}"
    }

    private fun markServerMessage() {
        lastServerMessageAtMs = SystemClock.elapsedRealtime()
    }

    private fun isConnectionStale(): Boolean {
        if (!handshakeComplete) return false
        val lastMessageAt = lastServerMessageAtMs
        if (lastMessageAt <= 0L) return false
        return SystemClock.elapsedRealtime() - lastMessageAt > STALE_CONNECTION_MS
    }

    private fun nowUs(): Long = SystemClock.elapsedRealtimeNanos() / 1000L

    private fun parseTimestampUs(payload: ByteArray): Long {
        var value = 0L
        for (i in 1 until BINARY_HEADER_SIZE) {
            value = (value shl 8) or (payload[i].toLong() and 0xFF)
        }
        return value
    }

    private class SendspinTimeSync {
        private var lastErrorUs: Long = Long.MAX_VALUE
        private var synchronized = false

        fun update(errorUs: Long) {
            lastErrorUs = errorUs
            synchronized = true
        }

        fun intervalMs(): Long {
            if (!synchronized) return 200L
            return when {
                lastErrorUs < 1_000 -> 3_000L
                lastErrorUs < 2_000 -> 1_000L
                lastErrorUs < 5_000 -> 500L
                else -> 200L
            }
        }
    }

    companion object {
        private const val TAG = "SendspinPlayback"
        private const val SENDSPIN_PORT = 8927
        private const val BUFFER_CAPACITY_BYTES = 512 * 1024
        private const val AUDIO_CHUNK_MESSAGE_TYPE = 4
        private const val BINARY_HEADER_SIZE = 9
        private const val HANDSHAKE_TIMEOUT_MS = 10_000L
        private const val STATE_INTERVAL_MS = 10_000L
        private const val STALE_CONNECTION_MS = 60_000L
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
        private const val NORMAL_CLOSE_CODE = 1000
        private const val VOLUME_EPSILON = 0.001f
    }
}
