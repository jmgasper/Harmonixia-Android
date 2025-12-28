package com.harmonixia.android.ui.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.harmonixia.android.domain.model.PlaybackState
import com.harmonixia.android.service.playback.PlaybackServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay

sealed class PlaybackUiEvent {
    data class Error(val message: String) : PlaybackUiEvent()
}

@UnstableApi
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackServiceConnection: PlaybackServiceConnection
) : ViewModel() {
    val playbackState: StateFlow<PlaybackState> = playbackServiceConnection.playbackState
    val currentMediaItem = playbackServiceConnection.currentMediaItem
    private val playbackPositionTicks = playbackState
        .map { it == PlaybackState.PLAYING }
        .distinctUntilChanged()
        .flatMapLatest { isPlaying ->
            if (isPlaying) {
                flow {
                    emit(Unit)
                    while (true) {
                        delay(POSITION_TICK_MS)
                        emit(Unit)
                    }
                }
            } else {
                flowOf(Unit)
            }
        }
    val playbackPosition: StateFlow<Long> = combine(
        playbackServiceConnection.playbackPosition,
        playbackServiceConnection.mediaController,
        playbackPositionTicks
    ) { position, controller, _ ->
        (controller?.currentPosition ?: position).coerceAtLeast(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val queue = playbackServiceConnection.queue
    val volume: StateFlow<Float> = playbackServiceConnection.volume
        .map { it.coerceIn(0f, 1f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)
    private val queueState: StateFlow<QueueState> = combine(queue, currentMediaItem) { items, mediaItem ->
        val index = items.indexOfFirst { it.mediaId == mediaItem?.mediaId }
        QueueState(
            hasNext = index >= 0 && index < items.lastIndex,
            hasPrevious = index > 0
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        QueueState(hasNext = false, hasPrevious = false)
    )
    val hasNext: StateFlow<Boolean> = queueState
        .map { it.hasNext }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val hasPrevious: StateFlow<Boolean> = queueState
        .map { it.hasPrevious }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val duration: StateFlow<Long> = combine(
        playbackServiceConnection.mediaController,
        currentMediaItem
    ) { controller, mediaItem ->
        val controllerDuration = controller?.duration ?: 0L
        val metadataDuration = mediaItem?.mediaMetadata?.durationMs ?: 0L
        when {
            controllerDuration > 0L -> controllerDuration
            metadataDuration > 0L -> metadataDuration
            else -> 0L
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val nowPlayingUiState: StateFlow<NowPlayingUiState> = combine(
        playbackState,
        currentMediaItem,
        playbackPosition,
        duration,
        queueState
    ) { state, mediaItem, position, duration, queueState ->
        buildNowPlayingUiState(
            mediaItem = mediaItem,
            playbackState = state,
            currentPosition = position,
            duration = duration,
            hasNext = queueState.hasNext,
            hasPrevious = queueState.hasPrevious
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingUiState.Idle)
    val currentTimeFormatted: StateFlow<String> = playbackPosition
        .map { formatDuration(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), formatDuration(0L))
    val durationFormatted: StateFlow<String> = duration
        .map { formatDuration(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), formatDuration(0L))

    private val _events = MutableSharedFlow<PlaybackUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PlaybackUiEvent> = _events.asSharedFlow()

    init {
        playbackServiceConnection.connect()
    }

    fun play() {
        playbackServiceConnection.play()
            .onFailure { _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to play")) }
    }

    fun pause() {
        playbackServiceConnection.pause()
            .onFailure { _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to pause")) }
    }

    fun togglePlayPause() {
        if (playbackState.value == PlaybackState.PLAYING) {
            pause()
        } else {
            play()
        }
    }

    fun next() {
        playbackServiceConnection.next()
            .onFailure { _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to skip")) }
    }

    fun previous() {
        playbackServiceConnection.previous()
            .onFailure { _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to skip")) }
    }

    fun seek(positionMs: Long) {
        playbackServiceConnection.seek(positionMs)
            .onFailure { _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to seek")) }
    }

    fun setVolume(volume: Float) {
        val controller = playbackServiceConnection.mediaController.value
        if (controller == null) {
            _events.tryEmit(PlaybackUiEvent.Error("Playback not ready"))
            return
        }
        runCatching {
            controller.volume = volume.coerceIn(0f, 1f)
        }.onFailure {
            _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to set volume"))
        }
    }

    override fun onCleared() {
        playbackServiceConnection.disconnect()
    }

    private data class QueueState(
        val hasNext: Boolean,
        val hasPrevious: Boolean
    )

    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}

private const val POSITION_TICK_MS = 1_000L
