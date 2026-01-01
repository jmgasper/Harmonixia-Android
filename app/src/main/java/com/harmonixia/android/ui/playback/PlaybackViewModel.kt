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

sealed class PlaybackUiEvent {
    data class Error(val message: String) : PlaybackUiEvent()
}

@UnstableApi
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackServiceConnection: PlaybackServiceConnection,
    private val getPlayersUseCase: GetPlayersUseCase,
    private val playbackStateManager: PlaybackStateManager,
    settingsDataStore: SettingsDataStore
) : ViewModel() {
    val playbackState: StateFlow<PlaybackState> = playbackServiceConnection.playbackState
    val currentMediaItem = playbackServiceConnection.currentMediaItem
    val repeatMode: StateFlow<RepeatMode> = playbackServiceConnection.repeatMode
    val shuffle: StateFlow<Boolean> = playbackServiceConnection.shuffle
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
        currentMediaItem,
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
        NowPlayingInputs(
            playbackState = state,
            mediaItem = mediaItem,
            position = position,
            duration = duration,
            hasNext = queueState.hasNext,
            hasPrevious = queueState.hasPrevious,
            repeatMode = queueState.repeatMode,
            shuffle = queueState.shuffle
        )
    }.combine(selectedPlayer) { inputs, selectedPlayer ->
        buildNowPlayingUiState(
            mediaItem = inputs.mediaItem,
            playbackState = inputs.playbackState,
            currentPosition = inputs.position,
            duration = inputs.duration,
            hasNext = inputs.hasNext,
            hasPrevious = inputs.hasPrevious,
            repeatMode = inputs.repeatMode,
            shuffle = inputs.shuffle,
            selectedPlayer = selectedPlayer
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

    fun toggleRepeatMode() {
        if (_isRepeatModeUpdating.value) return
        val nextMode = when (repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _isRepeatModeUpdating.value = true
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
                repeatMode.first { it == nextMode }
            } finally {
                _isRepeatModeUpdating.value = false
            }
        }
    }

    fun toggleShuffle() {
        if (_isShuffleUpdating.value) return
        val nextShuffle = !shuffle.value
        _isShuffleUpdating.value = true
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
                shuffle.first { it == nextShuffle }
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
        val playbackState: PlaybackState,
        val mediaItem: MediaItem?,
        val position: Long,
        val duration: Long,
        val hasNext: Boolean,
        val hasPrevious: Boolean,
        val repeatMode: RepeatMode,
        val shuffle: Boolean
    )

    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}

private const val POSITION_TICK_MS = 1_000L
