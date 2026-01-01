package com.harmonixia.android.ui.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.domain.model.PlaybackState
import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.domain.usecase.GetPlayersUseCase
import com.harmonixia.android.service.playback.PlaybackServiceConnection
import com.harmonixia.android.service.playback.PlaybackStateManager
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.PlayerSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed class PlaybackUiEvent {
    data class Error(val message: String) : PlaybackUiEvent()
}

@UnstableApi
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackServiceConnection: PlaybackServiceConnection,
    private val getPlayersUseCase: GetPlayersUseCase,
    private val playbackStateManager: PlaybackStateManager,
    settingsDataStore: SettingsDataStore,
    val imageQualityManager: ImageQualityManager
) : ViewModel() {
    private val _optimisticPlaybackState = MutableStateFlow<PlaybackState?>(null)
    private val _optimisticRepeatMode = MutableStateFlow<RepeatMode?>(null)
    private val _optimisticShuffle = MutableStateFlow<Boolean?>(null)
    private val _optimisticMediaItem = MutableStateFlow<MediaItem?>(null)
    val playbackState: StateFlow<PlaybackState> = combine(
        playbackServiceConnection.playbackState,
        _optimisticPlaybackState
    ) { state, optimisticState ->
        optimisticState ?: state
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        playbackServiceConnection.playbackState.value
    )
    val currentMediaItem = playbackServiceConnection.currentMediaItem
    private val displayedMediaItem: StateFlow<MediaItem?> = combine(
        currentMediaItem,
        _optimisticMediaItem
    ) { mediaItem, optimisticMediaItem ->
        optimisticMediaItem ?: mediaItem
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        currentMediaItem.value
    )
    val repeatMode: StateFlow<RepeatMode> = combine(
        playbackServiceConnection.repeatMode,
        _optimisticRepeatMode
    ) { mode, optimisticMode ->
        optimisticMode ?: mode
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        playbackServiceConnection.repeatMode.value
    )
    val shuffle: StateFlow<Boolean> = combine(
        playbackServiceConnection.shuffle,
        _optimisticShuffle
    ) { shuffle, optimisticShuffle ->
        optimisticShuffle ?: shuffle
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        playbackServiceConnection.shuffle.value
    )
    val localPlayerId: StateFlow<String?> = settingsDataStore.getSendspinClientId()
        .map { it.trim().ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _availablePlayers = MutableStateFlow<List<Player>>(emptyList())
    val availablePlayers: StateFlow<List<Player>> = _availablePlayers.asStateFlow()
    private val _selectedPlayer = MutableStateFlow<Player?>(null)
    val selectedPlayer: StateFlow<Player?> = _selectedPlayer.asStateFlow()
    private val _isRepeatModeUpdating = MutableStateFlow(false)
    val isRepeatModeUpdating: StateFlow<Boolean> = _isRepeatModeUpdating.asStateFlow()
    private val _isShuffleUpdating = MutableStateFlow(false)
    val isShuffleUpdating: StateFlow<Boolean> = _isShuffleUpdating.asStateFlow()
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
    private val queueState: StateFlow<QueueState> = combine(
        queue,
        displayedMediaItem,
        repeatMode,
        shuffle
    ) { items, mediaItem, repeatMode, shuffle ->
        val index = items.indexOfFirst { it.mediaId == mediaItem?.mediaId }
        QueueState(
            hasNext = index >= 0 && index < items.lastIndex,
            hasPrevious = index > 0,
            repeatMode = repeatMode,
            shuffle = shuffle
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        QueueState(
            hasNext = false,
            hasPrevious = false,
            repeatMode = RepeatMode.OFF,
            shuffle = false
        )
    )
    val hasNext: StateFlow<Boolean> = queueState
        .map { it.hasNext }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val hasPrevious: StateFlow<Boolean> = queueState
        .map { it.hasPrevious }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val duration: StateFlow<Long> = combine(
        playbackServiceConnection.mediaController,
        displayedMediaItem
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
        displayedMediaItem,
        playbackPosition,
        duration,
        queueState
    ) { state, mediaItem, position, duration, queueState ->
        NowPlayingInputs(
            state = state,
            mediaItem = mediaItem,
            position = position,
            duration = duration,
            queueState = queueState
        )
    }.combine(selectedPlayer) { inputs, player ->
        buildNowPlayingUiState(
            mediaItem = inputs.mediaItem,
            playbackState = inputs.state,
            currentPosition = inputs.position,
            duration = inputs.duration,
            hasNext = inputs.queueState.hasNext,
            hasPrevious = inputs.queueState.hasPrevious,
            repeatMode = inputs.queueState.repeatMode,
            shuffle = inputs.queueState.shuffle,
            selectedPlayer = player
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
        refreshPlayers()
        viewModelScope.launch {
            var lastMissingPlayerId: String? = null
            playbackStateManager.playerIdFlow.collect { playerId ->
                if (playerId == null) return@collect
                if (playerId == _selectedPlayer.value?.playerId) return@collect
                val player = _availablePlayers.value.find { it.playerId == playerId }
                if (player != null) {
                    _selectedPlayer.value = player
                    lastMissingPlayerId = null
                } else if (playerId != lastMissingPlayerId) {
                    lastMissingPlayerId = playerId
                    refreshPlayers()
                }
            }
        }
    }

    fun refreshPlayers() {
        viewModelScope.launch {
            getPlayersUseCase()
                .onSuccess { players ->
                    val resolvedLocalId = localPlayerId.value
                    val sortedPlayers = sortPlayers(players, resolvedLocalId)
                    _availablePlayers.value = sortedPlayers
                    if (!playbackStateManager.hasExplicitPlayerSelection()) {
                        selectLocalPlayer(sortedPlayers, resolvedLocalId)
                        return@onSuccess
                    }
                    val playerId = playbackStateManager.currentPlayerId
                    if (playerId != null && playerId != _selectedPlayer.value?.playerId) {
                        val player = sortedPlayers.find { it.playerId == playerId }
                        if (player != null) {
                            _selectedPlayer.value = player
                        }
                    }
                }
        }
    }

    fun selectPlayer(player: Player) {
        _selectedPlayer.value = player
        playbackStateManager.setSelectedPlayer(player.playerId)
    }

    private fun selectLocalPlayer(players: List<Player>, localPlayerId: String?) {
        val resolvedLocalId = localPlayerId?.takeIf { it.isNotBlank() }
        val localPlayer = PlayerSelection.selectLocalPlayer(players, resolvedLocalId)
        if (localPlayer != null) {
            _selectedPlayer.value = localPlayer
            playbackStateManager.setSelectedPlayer(localPlayer.playerId, explicit = false)
            return
        }
        _selectedPlayer.value = null
    }

    private fun sortPlayers(players: List<Player>, localPlayerId: String?): List<Player> {
        if (players.isEmpty()) return players
        val resolvedLocalId = localPlayerId?.takeIf { it.isNotBlank() }
        val (local, others) = players.partition { player ->
            PlayerSelection.isLocalPlayer(player, resolvedLocalId)
        }
        return local + others
    }

    fun play() {
        _optimisticPlaybackState.value = PlaybackState.PLAYING
        clearOptimisticPlaybackState(PlaybackState.PLAYING, OPTIMISTIC_PLAYBACK_STATE_TIMEOUT_MS)
        playbackServiceConnection.play()
            .onFailure { _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to play")) }
    }

    fun pause() {
        _optimisticPlaybackState.value = PlaybackState.PAUSED
        clearOptimisticPlaybackState(PlaybackState.PAUSED, OPTIMISTIC_PLAYBACK_STATE_TIMEOUT_MS)
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
        setOptimisticQueueMove(1)
        playbackServiceConnection.next()
            .onFailure {
                _optimisticMediaItem.value = null
                _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to skip"))
            }
    }

    fun previous() {
        setOptimisticQueueMove(-1)
        playbackServiceConnection.previous()
            .onFailure {
                _optimisticMediaItem.value = null
                _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to skip"))
            }
    }

    fun seek(positionMs: Long) {
        playbackServiceConnection.seek(positionMs)
            .onFailure { _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to seek")) }
    }

    fun toggleRepeatMode() {
        if (_isRepeatModeUpdating.value) return
        val nextMode = when (repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _isRepeatModeUpdating.value = true
        _optimisticRepeatMode.value = nextMode
        clearOptimisticRepeatMode(nextMode, OPTIMISTIC_REPEAT_TIMEOUT_MS)
        viewModelScope.launch {
            try {
                val result = playbackServiceConnection.setRepeatMode(nextMode)
                if (result.isFailure) {
                    _events.tryEmit(
                        PlaybackUiEvent.Error(
                            result.exceptionOrNull()?.message ?: "Failed to set repeat mode"
                        )
                    )
                    return@launch
                }
            } finally {
                _isRepeatModeUpdating.value = false
            }
        }
    }

    fun toggleShuffle() {
        if (_isShuffleUpdating.value) return
        val nextShuffle = !shuffle.value
        _isShuffleUpdating.value = true
        _optimisticShuffle.value = nextShuffle
        clearOptimisticShuffle(nextShuffle, OPTIMISTIC_SHUFFLE_TIMEOUT_MS)
        viewModelScope.launch {
            try {
                val result = playbackServiceConnection.setShuffleMode(nextShuffle)
                if (result.isFailure) {
                    _events.tryEmit(
                        PlaybackUiEvent.Error(
                            result.exceptionOrNull()?.message ?: "Failed to set shuffle mode"
                        )
                    )
                    return@launch
                }
            } finally {
                _isShuffleUpdating.value = false
            }
        }
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
        val hasPrevious: Boolean,
        val repeatMode: RepeatMode,
        val shuffle: Boolean
    )

    private data class NowPlayingInputs(
        val state: PlaybackState,
        val mediaItem: MediaItem?,
        val position: Long,
        val duration: Long,
        val queueState: QueueState
    )

    private fun setOptimisticQueueMove(offset: Int) {
        val items = queue.value
        if (items.isEmpty()) return
        val currentItem = displayedMediaItem.value ?: return
        val currentIndex = items.indexOfFirst { it.mediaId == currentItem.mediaId }
        if (currentIndex == -1) return
        val targetIndex = currentIndex + offset
        if (targetIndex !in items.indices) return
        val targetItem = items[targetIndex]
        _optimisticMediaItem.value = targetItem
        clearOptimisticMediaItem(targetItem.mediaId, OPTIMISTIC_QUEUE_MOVE_TIMEOUT_MS)
    }

    private fun clearOptimisticPlaybackState(expectedState: PlaybackState, timeoutMs: Long) {
        viewModelScope.launch {
            withTimeoutOrNull(timeoutMs) {
                playbackServiceConnection.playbackState.first { it == expectedState }
            }
            if (_optimisticPlaybackState.value == expectedState) {
                _optimisticPlaybackState.value = null
            }
        }
    }

    private fun clearOptimisticRepeatMode(expectedMode: RepeatMode, timeoutMs: Long) {
        viewModelScope.launch {
            withTimeoutOrNull(timeoutMs) {
                playbackServiceConnection.repeatMode.first { it == expectedMode }
            }
            if (_optimisticRepeatMode.value == expectedMode) {
                _optimisticRepeatMode.value = null
            }
        }
    }

    private fun clearOptimisticShuffle(expectedShuffle: Boolean, timeoutMs: Long) {
        viewModelScope.launch {
            withTimeoutOrNull(timeoutMs) {
                playbackServiceConnection.shuffle.first { it == expectedShuffle }
            }
            if (_optimisticShuffle.value == expectedShuffle) {
                _optimisticShuffle.value = null
            }
        }
    }

    private fun clearOptimisticMediaItem(expectedMediaId: String, timeoutMs: Long) {
        viewModelScope.launch {
            withTimeoutOrNull(timeoutMs) {
                currentMediaItem.first { it?.mediaId == expectedMediaId }
            }
            if (_optimisticMediaItem.value?.mediaId == expectedMediaId) {
                _optimisticMediaItem.value = null
            }
        }
    }

    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}

private const val POSITION_TICK_MS = 1_000L
private const val OPTIMISTIC_PLAYBACK_STATE_TIMEOUT_MS = 2_000L
private const val OPTIMISTIC_REPEAT_TIMEOUT_MS = 3_000L
private const val OPTIMISTIC_SHUFFLE_TIMEOUT_MS = 3_000L
private const val OPTIMISTIC_QUEUE_MOVE_TIMEOUT_MS = 1_500L
