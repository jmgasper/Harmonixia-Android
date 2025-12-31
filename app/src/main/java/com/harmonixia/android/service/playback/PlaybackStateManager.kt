package com.harmonixia.android.service.playback

import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.harmonixia.android.domain.model.PlaybackState
import com.harmonixia.android.domain.model.Queue
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.PlayerSelection
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
    private var lastQueue: Queue? = null
    private val syncSeekLock = Any()
    private var pendingSyncSeeks = 0
    private var lastSyncSeekAtMs = 0L

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

    fun attachPlayer(player: ExoPlayer) {
        this.player = player
    }

    fun notifyUserInitiatedPlayback() {
        userInitiatedPlayback = true
        startupAutoPausePending = false
        suppressAutoPlay = false
    }

    fun start() {
        if (pollJob != null) return
        startupAutoPausePending = !userInitiatedPlayback
        suppressAutoPlay = false
        pollJob = scope.launch {
            while (isActive) {
                val playerId = resolvePlayerId()
                if (playerId != null) {
                    repository.getActiveQueue(playerId)
                        .onSuccess { queue ->
                            withContext(mainDispatcher) {
                                handleQueue(queue)
                            }
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
        synchronized(syncSeekLock) {
            pendingSyncSeeks = 0
            lastSyncSeekAtMs = 0L
        }
    }

    private suspend fun resolvePlayerId(): String? {
        val current = _playerId.value
        if (!current.isNullOrBlank()) return current
        val players = repository.fetchPlayers().getOrNull().orEmpty()
        val selected = PlayerSelection.selectLocalPlayer(players)
        if (selected == null) {
            if (!localPlayerMissingLogged) {
                Logger.w(TAG, "Local playback device not found; skipping remote players")
                localPlayerMissingLogged = true
            }
            _playerId.value = null
            return null
        }
        localPlayerMissingLogged = false
        _playerId.value = selected.playerId
        return selected.playerId
    }

    private suspend fun handleQueue(queue: Queue?) {
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
        if (suppressAutoPlay && queue.state == PlaybackState.PLAYING) {
            syncPlayerState(queue, allowPlay = false)
            return
        }
        if (suppressAutoPlay && queue.state != PlaybackState.PLAYING) {
            suppressAutoPlay = false
        }
        syncPlayerState(queue, allowPlay = true)
    }

    fun resolveTrack(mediaId: String?): Track? {
        return resolveTrackFromQueue(mediaId)
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
        var needsIndexSeek = false
        if (remoteTrack != null && remoteTrack.itemId != localMediaId) {
            val mediaItem = queueManager.buildMediaItem(remoteTrack)
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

        when (queue.state) {
            PlaybackState.PLAYING -> {
                if (allowPlay) {
                    if (!player.isPlaying) player.play()
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
    }
}
