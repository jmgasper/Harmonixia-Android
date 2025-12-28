package com.harmonixia.android.data.remote

import com.harmonixia.android.util.Logger
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

open class MusicAssistantWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sendMutex = Mutex()
    private val messageIdGenerator = AtomicInteger(1)
    private val pendingRequests = ConcurrentHashMap<Int, CompletableDeferred<JsonElement>>()
    private val pendingPayloads = ConcurrentHashMap<Int, RequestPayload>()
    private val pendingPartialResults = ConcurrentHashMap<Int, MutableList<JsonElement>>()
    private val queuedRequests = ArrayDeque<RequestPayload>()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _events = MutableSharedFlow<WebSocketMessage.EventMessage>(extraBufferCapacity = 64)

    open val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    open val events: SharedFlow<WebSocketMessage.EventMessage> = _events.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var serverUrl: String? = null
    private var authToken: String? = null
    private var reconnectJob: Job? = null
    private var connectDeferred: CompletableDeferred<Unit>? = null
    private var manualDisconnect = false
    private var reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Logger.i(TAG, "WebSocket connected")
            reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
            if (authToken.isNullOrBlank()) {
                scope.launch { markConnectedAndFlush() }
            } else {
                scope.launch { authenticateAndReady() }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleIncomingMessage(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            handleIncomingMessage(bytes.utf8())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val isAuthFailure = isAuthFailure(response = response, throwable = t)
            val message = if (isAuthFailure) AUTH_ERROR_MESSAGE else t.message ?: "WebSocket failure"
            Logger.e(TAG, message, t)
            _connectionState.value = ConnectionState.Error(message)
            if (isAuthFailure) {
                connectDeferred?.completeExceptionally(IllegalStateException(message))
                connectDeferred = null
                reconnectJob?.cancel()
                failPendingRequests(message)
                return
            }
            connectDeferred?.completeExceptionally(t)
            connectDeferred = null
            scheduleReconnect()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Logger.w(TAG, "WebSocket closing: $code $reason")
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Logger.i(TAG, "WebSocket closed: $code $reason")
            if (isAuthFailure(closeCode = code, reason = reason)) {
                _connectionState.value = ConnectionState.Error(AUTH_ERROR_MESSAGE)
                connectDeferred?.completeExceptionally(IllegalStateException(AUTH_ERROR_MESSAGE))
                connectDeferred = null
                reconnectJob?.cancel()
                failPendingRequests(AUTH_ERROR_MESSAGE)
                return
            }
            _connectionState.value = ConnectionState.Disconnected
            if (!manualDisconnect) {
                scheduleReconnect()
            }
        }
    }

    open suspend fun connect(serverUrl: String, authToken: String?): Result<Unit> {
        this.serverUrl = serverUrl
        this.authToken = authToken
        manualDisconnect = false
        reconnectJob?.cancel()
        closeWebSocket("Reconnecting")
        return connectInternal()
    }

    open suspend fun disconnect() {
        manualDisconnect = true
        reconnectJob?.cancel()
        connectDeferred?.cancel()
        connectDeferred = null
        closeWebSocket("Manual disconnect")
        failPendingRequests("Disconnected")
        _connectionState.value = ConnectionState.Disconnected
    }

    open suspend fun sendRequest(
        command: String,
        params: Map<String, Any?> = emptyMap()
    ): Result<JsonElement> {
        val messageId = messageIdGenerator.getAndIncrement()
        val deferred = CompletableDeferred<JsonElement>()
        val payload = RequestPayload(messageId, command, params)
        pendingRequests[messageId] = deferred
        pendingPayloads[messageId] = payload
        Logger.d(TAG, "Prepared request id=$messageId command=$command params=$params")
        queueOrSend(payload)
        return runCatching { withTimeout(REQUEST_TIMEOUT_MS) { deferred.await() } }
            .onFailure { error ->
                if (error is TimeoutCancellationException) {
                    pendingRequests.remove(messageId)
                    pendingPayloads.remove(messageId)
                    pendingPartialResults.remove(messageId)
                    if (!deferred.isCompleted) {
                        deferred.completeExceptionally(error)
                    }
                }
            }
    }

    private suspend fun connectInternal(): Result<Unit> {
        val url = serverUrl
            ?: return Result.failure(IllegalStateException("Server URL is not set"))
        val webSocketUrl = buildWebSocketUrl(url)
        _connectionState.value = ConnectionState.Connecting
        val deferred = CompletableDeferred<Unit>()
        connectDeferred = deferred
        val requestBuilder = Request.Builder().url(webSocketUrl)
        if (!authToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $authToken")
        }
        webSocket = okHttpClient.newWebSocket(requestBuilder.build(), webSocketListener)
        val result = runCatching { withTimeout(CONNECT_TIMEOUT_MS) { deferred.await() } }
        if (result.isFailure) {
            val message = result.exceptionOrNull()?.message ?: "Connection timeout"
            _connectionState.value = ConnectionState.Error(message)
            connectDeferred?.cancel()
            connectDeferred = null
        }
        return result
    }

    private fun buildWebSocketUrl(serverUrl: String): String {
        val trimmed = serverUrl.trim().trimEnd('/')
        val wsBase = when {
            trimmed.startsWith("https://") -> "wss://${trimmed.removePrefix("https://")}"
            trimmed.startsWith("http://") -> "ws://${trimmed.removePrefix("http://")}"
            trimmed.startsWith("wss://") || trimmed.startsWith("ws://") -> trimmed
            else -> "ws://$trimmed"
        }
        return if (wsBase.endsWith("/ws")) wsBase else "$wsBase/ws"
    }

    private suspend fun queueOrSend(payload: RequestPayload) {
        sendMutex.withLock {
            val socket = webSocket
            val canSend = socket != null &&
                (_connectionState.value is ConnectionState.Connected || payload.command == AUTH_COMMAND)
            if (!canSend) {
                queuedRequests.add(payload)
                Logger.d(
                    TAG,
                    "Queued request id=${payload.messageId} command=${payload.command} state=${_connectionState.value}"
                )
                return
            }
            Logger.d(TAG, "Sending request id=${payload.messageId} command=${payload.command}")
            sendPayload(socket, payload)
        }
    }

    private fun sendPayload(webSocket: WebSocket, payload: RequestPayload) {
        val messageJson = buildJsonObject {
            put("message_id", JsonPrimitive(payload.messageId.toString()))
            put("command", JsonPrimitive(payload.command))
            if (payload.params.isNotEmpty()) {
                put("args", paramsToJson(payload.params))
            }
        }
        val payloadString = json.encodeToString(JsonObject.serializer(), messageJson)
        Logger.d(TAG, "-> $payloadString")
        if (!webSocket.send(payloadString)) {
            Logger.w(TAG, "WebSocket send failed, queueing request ${payload.messageId}")
            queuedRequests.addFirst(payload)
            scheduleReconnect()
        }
    }

    private fun handleIncomingMessage(rawMessage: String) {
        Logger.d(TAG, "<- $rawMessage")
        val parsed = runCatching { json.parseToJsonElement(rawMessage) }.getOrElse { error ->
            Logger.w(TAG, "Failed to parse message: $rawMessage", error)
            return
        }
        val messageObject = parsed as? JsonObject ?: return
        val messageId = parseMessageId(messageObject)
        if (messageId != null) {
            handleResponse(messageId, messageObject)
        } else {
            val eventName = messageObject["event"]?.jsonPrimitive?.contentOrNull
            if (eventName != null) {
                Logger.d(TAG, "Event received: $eventName")
                _events.tryEmit(WebSocketMessage.EventMessage(eventName, messageObject["data"]))
            }
        }
    }

    private fun handleResponse(messageId: Int, payload: JsonObject) {
        val deferred = pendingRequests[messageId]
        if (deferred == null) {
            Logger.w(TAG, "No pending request for message_id=$messageId payload=$payload")
            return
        }
        val errorMessage = extractErrorMessage(payload)
        if (errorMessage != null) {
            pendingRequests.remove(messageId)
            pendingPayloads.remove(messageId)
            pendingPartialResults.remove(messageId)
            deferred.completeExceptionally(IllegalStateException(errorMessage))
            return
        }
        val isPartial = payload["partial"]?.jsonPrimitive?.booleanOrNull == true
        val result = payload["result"] ?: JsonNull
        if (isPartial) {
            pendingPartialResults.getOrPut(messageId) { mutableListOf() }.add(result)
            return
        }
        pendingRequests.remove(messageId)
        pendingPayloads.remove(messageId)
        val merged = mergePartialResults(messageId, result)
        deferred.complete(merged)
    }

    private fun scheduleReconnect() {
        if (manualDisconnect || reconnectJob?.isActive == true) return
        if (serverUrl == null) return
        reconnectJob = scope.launch {
            while (!manualDisconnect) {
                delay(reconnectDelayMs)
                Logger.i(TAG, "Reconnecting in ${reconnectDelayMs}ms")
                val result = connectInternal()
                if (result.isSuccess) {
                    return@launch
                }
                reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            }
        }
    }

    private suspend fun enqueuePendingPayloads() {
        sendMutex.withLock {
            val pending = pendingPayloads.values.sortedBy { it.messageId }
            for (payload in pending) {
                if (queuedRequests.none { it.messageId == payload.messageId }) {
                    queuedRequests.add(payload)
                }
            }
        }
    }

    private suspend fun flushQueuedRequests() {
        sendMutex.withLock {
            val socket = webSocket ?: return
            while (queuedRequests.isNotEmpty()) {
                sendPayload(socket, queuedRequests.removeFirst())
            }
        }
    }

    private fun paramsToJson(params: Map<String, Any?>): JsonObject {
        return buildJsonObject {
            params.forEach { (key, value) ->
                put(key, anyToJsonElement(value))
            }
        }
    }

    private fun anyToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> {
                val mapped = value.entries.associate { (k, v) ->
                    k.toString() to anyToJsonElement(v)
                }
                JsonObject(mapped)
            }
            is Iterable<*> -> {
                JsonArray(value.map { anyToJsonElement(it) })
            }
            else -> JsonPrimitive(value.toString())
        }
    }

    private fun closeWebSocket(reason: String) {
        webSocket?.close(NORMAL_CLOSE_CODE, reason)
        webSocket = null
    }

    private fun failPendingRequests(message: String) {
        val exception = IllegalStateException(message)
        pendingRequests.values.forEach { it.completeExceptionally(exception) }
        pendingRequests.clear()
        pendingPayloads.clear()
        queuedRequests.clear()
        pendingPartialResults.clear()
    }

    private suspend fun authenticateAndReady() {
        val token = authToken?.trim().orEmpty()
        if (token.isBlank()) {
            markConnectedAndFlush()
            return
        }
        val result = sendRequest(AUTH_COMMAND, mapOf("token" to token))
        result.onSuccess {
            markConnectedAndFlush()
        }.onFailure { error ->
            val message = error.message ?: AUTH_ERROR_MESSAGE
            _connectionState.value = ConnectionState.Error(message)
            connectDeferred?.completeExceptionally(error)
            connectDeferred = null
            failPendingRequests(message)
        }
    }

    private suspend fun markConnectedAndFlush() {
        _connectionState.value = ConnectionState.Connected
        connectDeferred?.complete(Unit)
        connectDeferred = null
        enqueuePendingPayloads()
        flushQueuedRequests()
    }

    private fun parseMessageId(messageObject: JsonObject): Int? {
        val raw = messageObject["message_id"] ?: return null
        val primitive = raw.jsonPrimitive
        return primitive.intOrNull ?: primitive.contentOrNull?.toIntOrNull()
    }

    private fun extractErrorMessage(payload: JsonObject): String? {
        val errorCode = payload["error_code"]?.jsonPrimitive?.intOrNull
        if (errorCode != null) {
            return payload["details"]?.jsonPrimitive?.contentOrNull ?: "Server error ($errorCode)"
        }
        val error = payload["error"]
        if (error != null && error !is JsonNull) {
            return when (error) {
                is JsonPrimitive -> error.content
                is JsonObject -> error["message"]?.jsonPrimitive?.contentOrNull ?: error.toString()
                is JsonArray -> error.toString()
                else -> error.toString()
            }
        }
        return null
    }

    private fun mergePartialResults(messageId: Int, result: JsonElement): JsonElement {
        val partials = pendingPartialResults.remove(messageId) ?: return result
        val combined = mutableListOf<JsonElement>()
        partials.forEach { partial ->
            when (partial) {
                is JsonArray -> combined.addAll(partial)
                is JsonNull -> Unit
                else -> combined.add(partial)
            }
        }
        when (result) {
            is JsonArray -> combined.addAll(result)
            is JsonNull -> Unit
            else -> combined.add(result)
        }
        return JsonArray(combined)
    }

    private fun isAuthFailure(
        response: Response? = null,
        closeCode: Int? = null,
        reason: String? = null,
        throwable: Throwable? = null
    ): Boolean {
        val httpCode = response?.code
        if (httpCode == HTTP_UNAUTHORIZED || httpCode == HTTP_FORBIDDEN) return true
        if (closeCode == HTTP_UNAUTHORIZED || closeCode == HTTP_FORBIDDEN) return true
        val message = sequenceOf(response?.message, reason, throwable?.message)
            .filterNotNull()
            .joinToString(" ")
            .lowercase()
        if (message.isBlank()) return false
        return AUTH_ERROR_HINTS.any { message.contains(it) }
    }

    private data class RequestPayload(
        val messageId: Int,
        val command: String,
        val params: Map<String, Any?>
    )

    private companion object {
        private const val TAG = "MAWebSocket"
        private const val AUTH_COMMAND = "auth"
        private const val NORMAL_CLOSE_CODE = 1000
        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val REQUEST_TIMEOUT_MS = 20_000L
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val AUTH_ERROR_MESSAGE = "Authentication failed. Please update your token."
        private val AUTH_ERROR_HINTS = listOf(
            "auth failed",
            "unauthorized",
            "authorization",
            "forbidden",
            "invalid token",
            "invalid_token",
            "authentication"
        )
    }
}
