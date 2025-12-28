package com.harmonixia.android.service.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.harmonixia.android.domain.model.PlaybackState
import com.harmonixia.android.domain.model.Queue
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
    private var player: ExoPlayer? = null
    private var localPlayerMissingLogged = false
    private var startupAutoPausePending = false
    private var suppressAutoPlay = false
    private var userInitiatedPlayback = false

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playerId = MutableStateFlow<String?>(null)
    val playerIdFlow: StateFlow<String?> = _playerId.asStateFlow()

    private val _queueId = MutableStateFlow<String?>(null)
    val queueIdFlow: StateFlow<String?> = _queueId.asStateFlow()

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
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        startupAutoPausePending = false
        suppressAutoPlay = false
        userInitiatedPlayback = false
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

    private fun handleQueue(queue: Queue?) {
        if (queue == null) {
            _queueId.value = null
            _playbackState.value = PlaybackState.IDLE
            return
        }
        _queueId.value = queue.queueId
        _playbackState.value = queue.state
        queueManager.updateFromRemote(queue)
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

    private fun syncPlayerState(queue: Queue, allowPlay: Boolean) {
        val player = player ?: return
        val remoteTrack = queue.currentItem
        val localMediaId = player.currentMediaItem?.mediaId
        if (remoteTrack != null && remoteTrack.itemId != localMediaId) {
            val mediaItem = queueManager.buildMediaItem(remoteTrack)
            player.setMediaItem(mediaItem)
            player.prepare()
        } else if (player.mediaItemCount > 0 && player.currentMediaItemIndex != queue.currentIndex) {
            player.seekTo(queue.currentIndex, C.TIME_UNSET)
        }

        val remotePositionMs = queue.elapsedTime * 1000L
        val localPositionMs = player.currentPosition
        if (abs(remotePositionMs - localPositionMs) > POSITION_TOLERANCE_MS) {
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

    companion object {
        private const val TAG = "PlaybackStateManager"
        private const val POLL_INTERVAL_MS = 2000L
        private const val POSITION_TOLERANCE_MS = 2000L
    }
}
