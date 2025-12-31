package com.harmonixia.android.service.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.harmonixia.android.domain.model.PlaybackState
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class PlaybackServiceConnection(
    private val context: Context,
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controllerState = MutableStateFlow<MediaController?>(null)
    val mediaController: StateFlow<MediaController?> = controllerState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private var positionJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentMediaItem.value = mediaItem
            updateQueue()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            updateQueue()
        }

        override fun onVolumeChanged(volume: Float) {
            _volume.value = volume.coerceIn(0f, 1f)
        }
    }

    init {
        scope.launch {
            playbackStateManager.repeatMode.collect { _repeatMode.value = it }
        }
        scope.launch {
            playbackStateManager.shuffle.collect { _shuffle.value = it }
        }
    }

    fun connect() {
        if (controllerState.value != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        attachController(future)
    }

    fun disconnect() {
        val controller = controllerState.value ?: return
        controller.removeListener(playerListener)
        controller.release()
        controllerState.value = null
        _playbackState.value = PlaybackState.IDLE
        _currentMediaItem.value = null
        _playbackPosition.value = 0L
        _queue.value = emptyList()
        _repeatMode.value = RepeatMode.OFF
        _shuffle.value = false
        _volume.value = 1f
        stopPositionUpdates()
    }

    fun play(): Result<Unit> {
        val controller = controllerState.value ?: return Result.failure(IllegalStateException("Not connected"))
        controller.play()
        return Result.success(Unit)
    }

    fun pause(): Result<Unit> {
        val controller = controllerState.value ?: return Result.failure(IllegalStateException("Not connected"))
        controller.pause()
        return Result.success(Unit)
    }

    fun next(): Result<Unit> {
        val controller = controllerState.value ?: return Result.failure(IllegalStateException("Not connected"))
        controller.seekToNext()
        return Result.success(Unit)
    }

    fun previous(): Result<Unit> {
        val controller = controllerState.value ?: return Result.failure(IllegalStateException("Not connected"))
        controller.seekToPrevious()
        return Result.success(Unit)
    }

    fun seek(positionMs: Long): Result<Unit> {
        val controller = controllerState.value ?: return Result.failure(IllegalStateException("Not connected"))
        controller.seekTo(positionMs)
        return Result.success(Unit)
    }

    suspend fun setRepeatMode(repeatMode: RepeatMode): Result<Unit> {
        val queueId = playbackStateManager.currentQueueId
            ?: return Result.failure(IllegalStateException("Queue ID unavailable"))
        return withContext(Dispatchers.IO) {
            repository.setRepeatMode(queueId, repeatMode)
        }
    }

    suspend fun setShuffleMode(shuffle: Boolean): Result<Unit> {
        val queueId = playbackStateManager.currentQueueId
            ?: return Result.failure(IllegalStateException("Queue ID unavailable"))
        return withContext(Dispatchers.IO) {
            repository.setShuffleMode(queueId, shuffle)
        }
    }

    private fun attachController(future: ListenableFuture<MediaController>) {
        val executor = ContextCompat.getMainExecutor(context)
        future.addListener(
            {
                try {
                    val controller = future.get()
                    controller.addListener(playerListener)
                    controllerState.value = controller
                    _currentMediaItem.value = controller.currentMediaItem
                    _volume.value = controller.volume.coerceIn(0f, 1f)
                    updatePlaybackState()
                    updateQueue()
                    startPositionUpdates()
                } catch (error: Exception) {
                    Logger.w(TAG, "Failed to connect to playback service", error)
                }
            },
            executor
        )
    }

    private fun startPositionUpdates() {
        if (positionJob != null) return
        positionJob = scope.launch {
            try {
                while (true) {
                    val controller = controllerState.value
                    if (controller == null) {
                        _playbackPosition.value = 0L
                        break
                    }
                    _playbackPosition.value = controller.currentPosition
                    delay(POSITION_UPDATE_INTERVAL_MS)
                }
            } finally {
                positionJob = null
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun updatePlaybackState() {
        val controller = controllerState.value ?: return
        val hasMedia = controller.currentMediaItem != null || controller.mediaItemCount > 0
        _playbackState.value = when {
            !hasMedia -> PlaybackState.IDLE
            controller.playWhenReady -> PlaybackState.PLAYING
            else -> PlaybackState.PAUSED
        }
    }

    private fun updateQueue() {
        val controller = controllerState.value ?: return
        val mediaItems = (0 until controller.mediaItemCount).map { index ->
            controller.getMediaItemAt(index)
        }
        _queue.value = mediaItems
    }

    companion object {
        private const val TAG = "PlaybackServiceConn"
        private const val POSITION_UPDATE_INTERVAL_MS = 1000L
    }
}
