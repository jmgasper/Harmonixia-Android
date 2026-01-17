package com.harmonixia.android.service.playback

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.harmonixia.android.domain.model.PlaybackState
import com.harmonixia.android.domain.model.Queue
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.PlayerSelection
import com.harmonixia.android.util.toPlaybackMediaItem
import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackStateManager(
    private val repository: MusicAssistantRepository,
    private val queueManager: QueueManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var pollJob: Job? = null
    private var progressJob: Job? = null
    private var player: ExoPlayer? = null
    private var localPlayerMissingLogged = false
    private var startupAutoPausePending = false
    private var suppressAutoPlay = false
    private var userInitiatedPlayback = false
    private var userPaused = false
    private var lastUserPlaybackRequestAtMs = 0L
    private var lastQueue: Queue? = null
    private val syncSeekLock = Any()
    private var pendingSyncSeeks = 0
    private var lastSyncSeekAtMs = 0L
    private val playerSelectionLock = Any()
    private var isPlayerExplicitlySelected = false
    private val pendingStartLock = Any()
    private var pendingStartMediaId: String? = null
    private var pendingStartUntilMs: Long = 0L
    private var autoPlaySuppressionLogKey: String? = null

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playerId = MutableStateFlow<String?>(null)
    val playerIdFlow: StateFlow<String?> = _playerId.asStateFlow()

    private val _queueId = MutableStateFlow<String?>(null)
    val queueIdFlow: StateFlow<String?> = _queueId.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    val currentPlayerId: String? get() = _playerId.value
    val currentQueueId: String? get() = _queueId.value

    fun isLocalPlaybackActive(): Boolean = queueManager.isLocalQueueActive()

    fun hasExplicitPlayerSelection(): Boolean {
        return synchronized(playerSelectionLock) { isPlayerExplicitlySelected }
    }

    fun attachPlayer(player: ExoPlayer) {
        this.player = player
    }

    fun notifyUserInitiatedPlayback() {
        userInitiatedPlayback = true
        startupAutoPausePending = false
        suppressAutoPlay = false
        userPaused = false
        lastUserPlaybackRequestAtMs = SystemClock.elapsedRealtime()
    }

    fun notifyUserInitiatedPause() {
        userPaused = true
        clearAutoPlaySuppressionLog()
        Logger.i(TAG, "User pause requested; suppressing auto-resume until explicit play")
    }

    fun seedQueue(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        if (safeIndex > 0) {
            registerPendingStart(tracks[safeIndex].itemId)
        } else {
            clearPendingStart()
        }
        val mediaItems = tracks.map { it.toPlaybackMediaItem() }
        scope.launch {
            withContext(mainDispatcher) {
                queueManager.replaceQueue(mediaItems, safeIndex, C.TIME_UNSET)
            }
        }
    }

    fun registerPendingStart(mediaId: String?) {
        val resolvedId = mediaId?.takeIf { it.isNotBlank() }
        synchronized(pendingStartLock) {
            pendingStartMediaId = resolvedId
            pendingStartUntilMs = if (resolvedId == null) 0L else {
                SystemClock.elapsedRealtime() + PENDING_START_RETENTION_MS
            }
        }
    }

    fun clearPendingStart() {
        synchronized(pendingStartLock) {
            pendingStartMediaId = null
            pendingStartUntilMs = 0L
        }
    }

    suspend fun refreshQueueNow() {
        val result = withContext(ioDispatcher) {
            val playerId = resolvePlayerId() ?: return@withContext null
            playerId to repository.getActiveQueue(playerId)
        } ?: return
        val (playerId, queueResult) = result
        queueResult
            .onSuccess { queue ->
                applyQueueUpdate(playerId, queue, allowPartialItems = false)
            }
            .onFailure { error ->
                Logger.w(TAG, "Failed to refresh queue state", error)
            }
    }

    fun setSelectedPlayer(playerId: String, explicit: Boolean = true) {
        val previousPlayerId: String?
        val shouldRefresh: Boolean
        synchronized(playerSelectionLock) {
            previousPlayerId = _playerId.value
            _playerId.value = playerId
            if (explicit) {
                isPlayerExplicitlySelected = true
            }
            shouldRefresh = previousPlayerId != null && previousPlayerId != playerId
        }

        if (previousPlayerId != null && previousPlayerId != playerId) {
            Logger.i(TAG, "Player switched from $previousPlayerId to $playerId")
            _queueId.value = null
            _playbackState.value = PlaybackState.IDLE
            lastQueue = null
            queueManager.updateQueueId(null)
            val localPlayer = player
            if (localPlayer?.isPlaying == true) {
                scope.launch(mainDispatcher) {
                    localPlayer.pause()
                }
            }
            if (shouldRefresh) {
                refreshQueueFast()
            }
        }
    }

    fun start() {
        if (pollJob != null) return
        startupAutoPausePending = !userInitiatedPlayback
        suppressAutoPlay = false
        scope.launch {
            val shouldResolve = synchronized(playerSelectionLock) {
                !isPlayerExplicitlySelected && _playerId.value.isNullOrBlank()
            }
            if (shouldResolve) {
                resolvePlayerId()
            }
        }
        pollJob = scope.launch {
            while (isActive) {
                val playerId = resolvePlayerId()
                if (playerId != null) {
                    repository.getActiveQueue(playerId)
                        .onSuccess { queue ->
                            applyQueueUpdate(playerId, queue, allowPartialItems = false)
                        }
                        .onFailure { error ->
                            Logger.w(TAG, "Failed to fetch queue state", error)
                        }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
        progressJob = scope.launch {
            while (isActive) {
                reportPlaybackProgress()
                delay(PROGRESS_REPORT_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        progressJob?.cancel()
        progressJob = null
        startupAutoPausePending = false
        suppressAutoPlay = false
        userInitiatedPlayback = false
        userPaused = false
        lastUserPlaybackRequestAtMs = 0L
        clearPendingStart()
        synchronized(syncSeekLock) {
            pendingSyncSeeks = 0
            lastSyncSeekAtMs = 0L
        }
        synchronized(playerSelectionLock) {
            isPlayerExplicitlySelected = false
        }
        clearAutoPlaySuppressionLog()
    }

    private suspend fun resolvePlayerId(): String? {
        val explicitlySelected = synchronized(playerSelectionLock) { isPlayerExplicitlySelected }
        val current = _playerId.value
        if (explicitlySelected) return current
        if (!current.isNullOrBlank()) return current
        val players = repository.fetchPlayers().getOrNull().orEmpty()
        val selected = PlayerSelection.selectLocalPlayer(players)
        if (selected == null) {
            if (!localPlayerMissingLogged) {
                Logger.w(TAG, "Local playback device not found; skipping remote players")
                localPlayerMissingLogged = true
            }
            val resolved = synchronized(playerSelectionLock) {
                if (!isPlayerExplicitlySelected) {
                    _playerId.value = null
                }
                _playerId.value
            }
            return resolved
        }
        localPlayerMissingLogged = false
        return synchronized(playerSelectionLock) {
            if (!isPlayerExplicitlySelected) {
                _playerId.value = selected.playerId
            }
            _playerId.value
        }
    }

    fun refreshQueue() {
        val playerId = _playerId.value ?: return
        scope.launch {
            repository.getActiveQueue(playerId)
                .onSuccess { queue ->
                    applyQueueUpdate(playerId, queue, allowPartialItems = false)
                }
                .onFailure { error ->
                    Logger.w(TAG, "Failed to refresh queue state", error)
                }
        }
    }

    fun refreshQueueFast() {
        val playerId = _playerId.value ?: return
        scope.launch {
            // Fetch queue state first for a quicker UI update, then hydrate full items.
            repository.getActiveQueue(playerId, includeItems = false)
                .onSuccess { queue ->
                    applyQueueUpdate(playerId, queue, allowPartialItems = true)
                }
                .onFailure { error ->
                    Logger.w(TAG, "Failed to fetch queue state", error)
                }
            repository.getActiveQueue(playerId)
                .onSuccess { queue ->
                    applyQueueUpdate(playerId, queue, allowPartialItems = false)
                }
                .onFailure { error ->
                    Logger.w(TAG, "Failed to refresh queue state", error)
                }
        }
    }

    private suspend fun applyQueueUpdate(
        playerId: String,
        queue: Queue?,
        allowPartialItems: Boolean
    ) {
        if (_playerId.value != playerId) return
        val resolvedQueue = queue?.let { resolveQueueItems(it, allowPartialItems) }
        withContext(mainDispatcher) {
            handleQueue(resolvedQueue)
        }
    }

    private fun resolveQueueItems(queue: Queue, allowPartialItems: Boolean): Queue {
        if (!allowPartialItems || queue.items.isNotEmpty()) return queue
        val currentItem = queue.currentItem ?: return queue
        return queue.copy(items = listOf(currentItem), currentIndex = 0)
    }

    private suspend fun handleQueue(queue: Queue?) {
        if (queueManager.isLocalQueueActive()) return
        if (queue == null) {
            _queueId.value = null
            _playbackState.value = PlaybackState.IDLE
            lastQueue = null
            return
        }
        lastQueue = queue
        _queueId.value = queue.queueId
        _playbackState.value = queue.state
        queueManager.updateFromRemote(queue)
        _repeatMode.value = queue.repeatMode
        _shuffle.value = queue.shuffle
        if (startupAutoPausePending) {
            startupAutoPausePending = false
            if (queue.state == PlaybackState.PLAYING) {
                suppressAutoPlay = true
                requestStartupPause(queue.queueId)
            }
        }
        val suppressionReason = resolveAutoPlaySuppressionReason(queue.state)
        if (suppressionReason != null) {
            logAutoPlaySuppressed(suppressionReason, queue.queueId)
            syncPlayerState(queue, allowPlay = false)
            return
        }
        if (suppressAutoPlay && queue.state != PlaybackState.PLAYING) {
            suppressAutoPlay = false
        }
        clearAutoPlaySuppressionLog()
        syncPlayerState(queue, allowPlay = true)
    }

    fun resolveTrack(mediaId: String?): Track? {
        return resolveTrackFromQueue(mediaId)
    }

    fun findPlayableIndex(queue: Queue): Int? {
        val items = queue.items
        if (items.isEmpty()) {
            return if (queue.currentItem?.isAvailable == true) queue.currentIndex else null
        }
        val safeStart = queue.currentIndex.coerceIn(0, items.lastIndex)
        val forwardIndex = (safeStart until items.size).firstOrNull { index ->
            items[index].isAvailable
        }
        if (forwardIndex != null) return forwardIndex
        return (0 until safeStart).firstOrNull { index ->
            items[index].isAvailable
        }
    }

    fun findPlayableIndexFromCurrent(): Int? {
        val queue = lastQueue ?: return null
        return findPlayableIndex(queue)
    }

    fun consumeSyncSeekSuppression(): Boolean {
        synchronized(syncSeekLock) {
            if (pendingSyncSeeks <= 0) return false
            val now = SystemClock.elapsedRealtime()
            if (now - lastSyncSeekAtMs > SYNC_SEEK_SUPPRESSION_WINDOW_MS) {
                pendingSyncSeeks = 0
                return false
            }
            pendingSyncSeeks -= 1
            return true
        }
    }

    fun suppressNextRemoteSeek() {
        // Avoid echoing seeks back to the server when we've already dispatched one.
        markSyncSeek()
    }

    private fun resolveTrackFromQueue(mediaId: String?): Track? {
        val queue = lastQueue ?: return null
        if (!mediaId.isNullOrBlank()) {
            if (queue.currentItem?.itemId == mediaId) return queue.currentItem
            return queue.items.firstOrNull { it.itemId == mediaId }
        }
        return queue.currentItem ?: queue.items.getOrNull(queue.currentIndex)
    }

    private suspend fun reportPlaybackProgress() {
        val player = player ?: return
        val snapshot = withContext(mainDispatcher) {
            val mediaId = player.currentMediaItem?.mediaId
            val isPlaying = player.isPlaying
            val positionSeconds = (player.currentPosition / 1000L).toInt()
            PlaybackSnapshot(mediaId, isPlaying, positionSeconds)
        }
        if (!snapshot.isPlaying) return
        if (_playbackState.value != PlaybackState.PLAYING) return
        val queueId = _queueId.value ?: return
        val track = resolveTrackFromQueue(snapshot.mediaId) ?: return
        Logger.d(
            TAG,
            "Reporting playback progress queueId=$queueId mediaId=${track.itemId} position=${snapshot.positionSeconds}"
        )
        repository.reportPlaybackProgress(queueId, track, snapshot.positionSeconds)
            .onFailure { Logger.w(TAG, "Failed to report playback progress", it) }
    }

    private suspend fun syncPlayerState(queue: Queue, allowPlay: Boolean) {
        val player = player ?: return
        val remoteTrack = queue.currentItem
        val localMediaId = player.currentMediaItem?.mediaId
        if (shouldDeferRemoteSync(remoteTrack?.itemId)) {
            applyPlayState(queue.state, allowPlay, player)
            return
        }
        var needsIndexSeek = false
        if (remoteTrack != null && remoteTrack.itemId != localMediaId) {
            val resolveLocal = queue.state == PlaybackState.PLAYING
            val mediaItem = queueManager.buildMediaItem(remoteTrack, resolveLocal = resolveLocal)
            player.setMediaItem(mediaItem)
            player.prepare()
        } else if (player.mediaItemCount > 0 && player.currentMediaItemIndex != queue.currentIndex) {
            needsIndexSeek = true
        }

        val remotePositionMs = resolveRemotePositionMs(queue)
        val localPositionMs = player.currentPosition
        if (needsIndexSeek) {
            markSyncSeek()
            player.seekTo(queue.currentIndex, remotePositionMs)
        } else if (abs(remotePositionMs - localPositionMs) > POSITION_TOLERANCE_MS) {
            markSyncSeek()
            player.seekTo(remotePositionMs)
        }

        if (remoteTrack != null && queue.state == PlaybackState.PLAYING) {
            queueManager.ensureLocalForCurrentTrack(remoteTrack)
        }

        applyPlayState(queue.state, allowPlay, player)
    }

    private fun shouldDeferRemoteSync(remoteMediaId: String?): Boolean {
        synchronized(pendingStartLock) {
            val pendingId = pendingStartMediaId ?: return false
            val now = SystemClock.elapsedRealtime()
            if (now > pendingStartUntilMs) {
                pendingStartMediaId = null
                return false
            }
            if (pendingId == remoteMediaId) {
                pendingStartMediaId = null
                return false
            }
            return true
        }
    }

    private fun applyPlayState(state: PlaybackState, allowPlay: Boolean, player: ExoPlayer) {
        when (state) {
            PlaybackState.PLAYING -> {
                if (allowPlay) {
                    if (!player.isPlaying) {
                        if (!isUserPlaybackRequestRecent()) {
                            Logger.i(
                                TAG,
                                "Auto-resume via queue sync queueId=${_queueId.value.orEmpty()} " +
                                    "mediaId=${player.currentMediaItem?.mediaId.orEmpty()}"
                            )
                        }
                        player.play()
                    }
                } else if (player.isPlaying) {
                    player.pause()
                }
            }
            PlaybackState.PAUSED -> if (player.isPlaying) player.pause()
            PlaybackState.IDLE -> if (player.playbackState != Player.STATE_IDLE) player.pause()
        }
    }

    private fun requestStartupPause(queueId: String) {
        scope.launch {
            repository.pauseQueue(queueId)
                .onFailure { Logger.w(TAG, "Failed to pause queue on startup", it) }
        }
    }

    private fun resolveRemotePositionMs(queue: Queue): Long {
        val baseMs = queue.elapsedTime.coerceAtLeast(0) * 1000L
        val lastUpdatedSeconds = queue.elapsedTimeLastUpdated ?: return baseMs
        if (queue.state != PlaybackState.PLAYING) return baseMs
        val nowSeconds = System.currentTimeMillis() / 1000.0
        val deltaSeconds = (nowSeconds - lastUpdatedSeconds).coerceAtLeast(0.0)
        return (baseMs + (deltaSeconds * 1000.0)).toLong()
    }

    private fun markSyncSeek() {
        synchronized(syncSeekLock) {
            pendingSyncSeeks = (pendingSyncSeeks + 1).coerceAtMost(MAX_PENDING_SYNC_SEEKS)
            lastSyncSeekAtMs = SystemClock.elapsedRealtime()
        }
    }

    private fun resolveAutoPlaySuppressionReason(state: PlaybackState): String? {
        if (state != PlaybackState.PLAYING) return null
        return when {
            userPaused -> AUTO_PLAY_SUPPRESSION_USER_PAUSE
            suppressAutoPlay -> AUTO_PLAY_SUPPRESSION_STARTUP
            else -> null
        }
    }

    private fun logAutoPlaySuppressed(reason: String, queueId: String?) {
        val key = "$reason:${queueId.orEmpty()}"
        if (autoPlaySuppressionLogKey == key) return
        autoPlaySuppressionLogKey = key
        Logger.i(
            TAG,
            "Auto-play suppressed reason=$reason queueId=${queueId.orEmpty()}"
        )
    }

    private fun clearAutoPlaySuppressionLog() {
        autoPlaySuppressionLogKey = null
    }

    private fun isUserPlaybackRequestRecent(): Boolean {
        val last = lastUserPlaybackRequestAtMs
        if (last == 0L) return false
        return SystemClock.elapsedRealtime() - last < USER_PLAY_REQUEST_SUPPRESSION_MS
    }

    private data class PlaybackSnapshot(
        val mediaId: String?,
        val isPlaying: Boolean,
        val positionSeconds: Int
    )

    companion object {
        private const val TAG = "PlaybackStateManager"
        private const val POLL_INTERVAL_MS = 2000L
        private const val POSITION_TOLERANCE_MS = 2000L
        private const val PROGRESS_REPORT_INTERVAL_MS = 10000L
        private const val MAX_PENDING_SYNC_SEEKS = 3
        private const val SYNC_SEEK_SUPPRESSION_WINDOW_MS = 1500L
        private const val PENDING_START_RETENTION_MS = 3000L
        private const val USER_PLAY_REQUEST_SUPPRESSION_MS = 5000L
        private const val AUTO_PLAY_SUPPRESSION_USER_PAUSE = "user_pause"
        private const val AUTO_PLAY_SUPPRESSION_STARTUP = "startup_auto_pause"
    }
}
