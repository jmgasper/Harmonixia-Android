package com.harmonixia.android.ui.playback

import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.harmonixia.android.data.remote.WebSocketMessage
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.PlaybackState
import com.harmonixia.android.domain.model.PlaybackContext
import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.domain.model.ProviderBadge
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import com.harmonixia.android.domain.usecase.GetPlayersUseCase
import com.harmonixia.android.domain.usecase.ResolveProviderBadgeUseCase
import com.harmonixia.android.domain.usecase.SearchLibraryUseCase
import com.harmonixia.android.domain.usecase.SetPlayerMuteUseCase
import com.harmonixia.android.domain.usecase.SetPlayerVolumeUseCase
import com.harmonixia.android.service.playback.PlaybackServiceConnection
import com.harmonixia.android.service.playback.PlaybackStateManager
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.PlayerSelection
import com.harmonixia.android.util.EXTRA_PARENT_MEDIA_ID
import com.harmonixia.android.util.EXTRA_PROVIDER_DOMAINS
import com.harmonixia.android.util.EXTRA_PROVIDER_ID
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed class PlaybackUiEvent {
    data class Error(val message: String) : PlaybackUiEvent()
}

data class AlbumReference(
    val itemId: String,
    val provider: String
)

@UnstableApi
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackServiceConnection: PlaybackServiceConnection,
    private val getPlayersUseCase: GetPlayersUseCase,
    private val setPlayerVolumeUseCase: SetPlayerVolumeUseCase,
    private val setPlayerMuteUseCase: SetPlayerMuteUseCase,
    private val resolveProviderBadgeUseCase: ResolveProviderBadgeUseCase,
    private val searchLibraryUseCase: SearchLibraryUseCase,
    private val localMediaRepository: LocalMediaRepository,
    private val playbackStateManager: PlaybackStateManager,
    private val repository: MusicAssistantRepository,
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
    private val providerMetadata: StateFlow<ProviderMetadata?> = displayedMediaItem
        .map { mediaItem ->
            val extras = mediaItem?.mediaMetadata?.extras ?: return@map null
            val providerKey = extras.getString(EXTRA_PROVIDER_ID)?.trim().orEmpty()
            val providerDomains = extras.getStringArray(EXTRA_PROVIDER_DOMAINS)
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.sorted()
                .orEmpty()
            if (providerKey.isBlank() && providerDomains.isEmpty()) {
                null
            } else {
                ProviderMetadata(providerKey.takeIf { it.isNotBlank() }, providerDomains)
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val providerBadge: StateFlow<ProviderBadge?> = providerMetadata
        .flatMapLatest { metadata ->
            if (metadata == null) {
                flowOf<ProviderBadge?>(null)
            } else {
                flow {
                    emit(
                        resolveProviderBadgeUseCase(
                            metadata.providerKey,
                            metadata.providerDomains
                        ).getOrNull()
                    )
                }
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
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
    val isPlayPauseUpdating: StateFlow<Boolean> = playbackServiceConnection.pendingPlaybackState
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val playbackContext: StateFlow<PlaybackContext?> = playbackStateManager.playbackContext
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
            hasPrevious = mediaItem != null,
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
        val metadataDuration = mediaItem?.mediaMetadata?.durationMs ?: 0L
        val controllerDuration = controller?.duration ?: 0L
        when {
            metadataDuration > 0L -> metadataDuration
            controllerDuration > 0L -> controllerDuration
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
            queueState = queueState,
            providerBadge = null
        )
    }.combine(providerBadge) { inputs, badge ->
        inputs.copy(providerBadge = badge)
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
            providerBadge = inputs.providerBadge,
            selectedPlayer = player
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingUiState.Idle)
    val currentTimeFormatted: StateFlow<String> = playbackPosition
        .map { formatDuration(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), formatDuration(0L))
    val durationFormatted: StateFlow<String> = duration
        .map { formatDuration(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), formatDuration(0L))

    private var playerRefreshJob: Job? = null
    private var lastPlayerRefreshAt = 0L

    private val _events = MutableSharedFlow<PlaybackUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PlaybackUiEvent> = _events.asSharedFlow()

    init {
        playbackServiceConnection.connect()
        refreshPlayers()
        viewModelScope.launch {
            repository.observeEvents().collect { event ->
                if (isPlayerEvent(event)) {
                    schedulePlayerRefresh()
                }
            }
        }
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
                    val displayPlayers = addLocalPlaceholderIfMissing(players, resolvedLocalId)
                    val sortedPlayers = sortPlayers(displayPlayers, resolvedLocalId)
                    _availablePlayers.value = sortedPlayers
                    if (!playbackStateManager.hasExplicitPlayerSelection()) {
                        selectLocalPlayer(players, resolvedLocalId)
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

    fun onAppResumed() {
        playbackServiceConnection.connect()
        refreshPlayers()
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

    private fun addLocalPlaceholderIfMissing(
        players: List<Player>,
        localPlayerId: String?
    ): List<Player> {
        val resolvedId = localPlayerId?.takeIf { it.isNotBlank() } ?: return players
        if (players.any { it.playerId == resolvedId }) return players
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val displayName = when {
            manufacturer.isNotBlank() && model.isNotBlank() -> "$manufacturer $model"
            model.isNotBlank() -> model
            manufacturer.isNotBlank() -> manufacturer
            else -> "Android Device"
        }
        val placeholder = Player(
            playerId = resolvedId,
            name = displayName,
            available = false,
            enabled = false,
            playbackState = PlaybackState.IDLE,
            volume = 0,
            volumeMuted = null,
            deviceManufacturer = manufacturer.ifBlank { null },
            deviceModel = model.ifBlank { null }
        )
        return listOf(placeholder) + players
    }

    private fun schedulePlayerRefresh() {
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - lastPlayerRefreshAt
        if (elapsed >= PLAYER_REFRESH_THROTTLE_MS) {
            lastPlayerRefreshAt = now
            refreshPlayers()
            return
        }
        if (playerRefreshJob?.isActive == true) return
        playerRefreshJob = viewModelScope.launch {
            delay(PLAYER_REFRESH_THROTTLE_MS - elapsed)
            lastPlayerRefreshAt = SystemClock.elapsedRealtime()
            refreshPlayers()
            playerRefreshJob = null
        }
    }

    private fun isPlayerEvent(event: WebSocketMessage.EventMessage): Boolean {
        return event.event.lowercase().startsWith("player_")
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
        val positionMs = (playbackServiceConnection.mediaController.value?.currentPosition
            ?: playbackPosition.value).coerceAtLeast(0L)
        val shouldSkipToPrevious = positionMs <= PREVIOUS_TRACK_THRESHOLD_MS && hasPreviousTrack()
        if (shouldSkipToPrevious) {
            setOptimisticQueueMove(-1)
            playbackServiceConnection.previous()
                .onFailure {
                    _optimisticMediaItem.value = null
                    _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to skip"))
                }
        } else {
            playbackServiceConnection.seek(0L)
                .onFailure { _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to seek")) }
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

    fun resolveNowPlayingArtist(onResolved: (Artist?) -> Unit) {
        val mediaItem = displayedMediaItem.value
        viewModelScope.launch {
            val artist = resolveArtistFromMediaItem(mediaItem)
            onResolved(artist)
        }
    }

    fun resolveNowPlayingAlbum(onResolved: (AlbumReference?) -> Unit) {
        val mediaItem = displayedMediaItem.value
        viewModelScope.launch {
            val albumReference = resolveAlbumFromMediaItem(mediaItem)
            onResolved(albumReference)
        }
    }

    private suspend fun resolveArtistFromMediaItem(mediaItem: MediaItem?): Artist? {
        val rawArtistName = mediaItem?.mediaMetadata?.artist?.toString().orEmpty()
        val artistName = rawArtistName.substringBefore(',').trim()
        if (artistName.isBlank()) return null
        val providerId = mediaItem?.mediaMetadata?.extras
            ?.getString(EXTRA_PROVIDER_ID)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return resolveArtistByName(artistName, providerId)
    }

    private suspend fun resolveAlbumFromMediaItem(mediaItem: MediaItem?): AlbumReference? {
        val metadata = mediaItem?.mediaMetadata ?: return null
        val extras = metadata.extras
        val parentMediaId = extras?.getString(EXTRA_PARENT_MEDIA_ID)?.trim()
        if (!parentMediaId.isNullOrBlank()) {
            parseAlbumParentId(parentMediaId)?.let { return it }
        }
        val albumName = metadata.albumTitle?.toString().orEmpty().trim()
        if (albumName.isBlank()) return null
        val rawArtistName = metadata.artist?.toString().orEmpty()
        val artistName = rawArtistName.substringBefore(',').trim()
        val providerId = extras?.getString(EXTRA_PROVIDER_ID)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val providerDomains = extras?.getStringArray(EXTRA_PROVIDER_DOMAINS)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            .orEmpty()
        return resolveAlbumByName(albumName, artistName, providerId, providerDomains)
    }

    private suspend fun resolveArtistByName(artistName: String, providerId: String?): Artist? =
        withContext(Dispatchers.IO) {
            val normalizedTarget = normalizeArtistName(artistName)
            if (normalizedTarget.isBlank()) {
                return@withContext null
            }
            if (providerId == OFFLINE_PROVIDER) {
                return@withContext buildOfflineArtist(artistName)
            }
            val searchResult = searchLibraryUseCase(
                artistName,
                ARTIST_LOOKUP_LIMIT,
                libraryOnly = true
            )
            if (searchResult.isSuccess) {
                val artists = searchResult.getOrNull()?.artists.orEmpty()
                val match = findMatchingArtist(artists, normalizedTarget, providerId)
                if (match != null) return@withContext match
                val expandedResult = searchLibraryUseCase(
                    artistName,
                    ARTIST_LOOKUP_LIMIT,
                    libraryOnly = false
                )
                if (expandedResult.isSuccess) {
                    val expandedArtists = expandedResult.getOrNull()?.artists.orEmpty()
                    val expandedMatch = findMatchingArtist(expandedArtists, normalizedTarget, providerId)
                    if (expandedMatch != null) return@withContext expandedMatch
                }
            }
            val localArtists = localMediaRepository.searchArtists(artistName).first()
            findMatchingArtist(localArtists, normalizedTarget, OFFLINE_PROVIDER)
        }

    private suspend fun resolveAlbumByName(
        albumName: String,
        artistName: String,
        providerId: String?,
        providerDomains: List<String>
    ): AlbumReference? = withContext(Dispatchers.IO) {
        val normalizedTarget = normalizeAlbumName(albumName)
        if (normalizedTarget.isBlank()) {
            return@withContext null
        }
        val preferredProviders = buildPreferredProviders(providerId, providerDomains)
        if (providerId == OFFLINE_PROVIDER) {
            val offlineMatch = resolveOfflineAlbum(albumName, artistName)
            if (offlineMatch != null) return@withContext offlineMatch
        }
        val libraryResult = searchLibraryUseCase(
            albumName,
            ALBUM_LOOKUP_LIMIT,
            libraryOnly = true
        )
        val libraryAlbums = if (libraryResult.isSuccess) {
            libraryResult.getOrNull()?.albums.orEmpty()
        } else {
            emptyList()
        }
        val libraryMatch = findMatchingAlbum(
            libraryAlbums,
            normalizedTarget,
            artistName,
            preferredProviders
        )
        val needsExpandedSearch = libraryMatch == null ||
            (preferredProviders.isNotEmpty() && libraryMatch.provider !in preferredProviders)
        if (!needsExpandedSearch && libraryMatch != null) {
            return@withContext AlbumReference(libraryMatch.itemId, libraryMatch.provider)
        }
        val expandedResult = searchLibraryUseCase(
            albumName,
            ALBUM_LOOKUP_LIMIT,
            libraryOnly = false
        )
        val expandedAlbums = if (expandedResult.isSuccess) {
            expandedResult.getOrNull()?.albums.orEmpty()
        } else {
            emptyList()
        }
        val combinedAlbums = if (expandedAlbums.isEmpty()) {
            libraryAlbums
        } else {
            libraryAlbums + expandedAlbums
        }
        val expandedMatch = findMatchingAlbum(
            combinedAlbums,
            normalizedTarget,
            artistName,
            preferredProviders
        )
        if (expandedMatch != null) {
            return@withContext AlbumReference(expandedMatch.itemId, expandedMatch.provider)
        }
        if (libraryMatch != null) {
            return@withContext AlbumReference(libraryMatch.itemId, libraryMatch.provider)
        }
        if (providerId != OFFLINE_PROVIDER) {
            val offlineMatch = resolveOfflineAlbum(albumName, artistName)
            if (offlineMatch != null) return@withContext offlineMatch
        }
        return@withContext null
    }

    private suspend fun resolveOfflineAlbum(
        albumName: String,
        artistName: String
    ): AlbumReference? {
        val trimmedAlbum = albumName.trim()
        val trimmedArtist = artistName.trim()
        if (trimmedAlbum.isBlank()) return null
        if (trimmedArtist.isNotBlank()) {
            val directMatch = localMediaRepository
                .getAlbumByNameAndArtist(trimmedAlbum, trimmedArtist)
                .first()
            if (directMatch != null) {
                return AlbumReference(directMatch.itemId, directMatch.provider)
            }
        }
        val localAlbums = localMediaRepository.searchAlbums(trimmedAlbum).first()
        val normalizedAlbum = normalizeAlbumName(trimmedAlbum)
        val normalizedArtist = normalizeArtistName(trimmedArtist)
        val match = localAlbums.firstOrNull { album ->
            normalizeAlbumName(album.name) == normalizedAlbum &&
                (normalizedArtist.isBlank() ||
                    album.artists.any { normalizeArtistName(it) == normalizedArtist })
        } ?: localAlbums.firstOrNull { album ->
            normalizeAlbumName(album.name) == normalizedAlbum
        }
        return match?.let { AlbumReference(it.itemId, it.provider) }
    }

    private fun findMatchingArtist(
        artists: List<Artist>,
        normalizedTarget: String,
        providerId: String?
    ): Artist? {
        if (artists.isEmpty()) return null
        val matches = artists.filter { normalizeArtistName(it.name) == normalizedTarget }
        if (matches.isEmpty()) return null
        if (!providerId.isNullOrBlank()) {
            matches.firstOrNull { it.provider == providerId }?.let { return it }
        }
        return matches.firstOrNull()
    }

    private fun findMatchingAlbum(
        albums: List<Album>,
        normalizedTarget: String,
        artistName: String,
        preferredProviders: Set<String>
    ): Album? {
        if (albums.isEmpty()) return null
        val normalizedArtist = normalizeArtistName(artistName)
        val albumMatches = albums.filter { normalizeAlbumName(it.name) == normalizedTarget }
        if (albumMatches.isEmpty()) return null
        val artistMatches = if (normalizedArtist.isNotBlank()) {
            albumMatches.filter { album ->
                album.artists.any { normalizeArtistName(it) == normalizedArtist }
            }.ifEmpty { albumMatches }
        } else {
            albumMatches
        }
        if (preferredProviders.isNotEmpty()) {
            artistMatches.firstOrNull { it.provider in preferredProviders }?.let { return it }
            albumMatches.firstOrNull { it.provider in preferredProviders }?.let { return it }
        }
        return artistMatches.firstOrNull() ?: albumMatches.firstOrNull()
    }

    private fun normalizeArtistName(name: String): String {
        return name.trim().lowercase()
    }

    private fun normalizeAlbumName(name: String): String {
        return name.trim().lowercase()
    }

    private fun buildPreferredProviders(
        providerId: String?,
        providerDomains: List<String>
    ): Set<String> {
        val preferred = LinkedHashSet<String>()
        providerId?.trim()?.takeIf { it.isNotBlank() }?.let { preferred.add(it) }
        for (domain in providerDomains) {
            val trimmed = domain.trim()
            if (trimmed.isNotBlank()) {
                preferred.add(trimmed)
            }
        }
        return preferred
    }

    private fun parseAlbumParentId(parentMediaId: String): AlbumReference? {
        val parts = parentMediaId.split(":", limit = 3)
        if (parts.size < 3 || parts[0] != MEDIA_ID_PREFIX_ALBUM) return null
        val albumId = parts[1].trim()
        val provider = parts[2].trim()
        if (albumId.isBlank() || provider.isBlank()) return null
        return AlbumReference(albumId, provider)
    }

    private fun buildOfflineArtist(name: String): Artist {
        val trimmed = name.trim()
        val encoded = Uri.encode(trimmed)
        return Artist(
            itemId = encoded,
            provider = OFFLINE_PROVIDER,
            uri = "offline:artist:$encoded",
            name = trimmed,
            sortName = trimmed.lowercase(),
            imageUrl = null
        )
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

    fun setPlayerVolume(player: Player, volume: Int) {
        val safeVolume = volume.coerceIn(0, 100)
        val players = _availablePlayers.value
        val index = players.indexOfFirst { it.playerId == player.playerId }
        if (index == -1) return
        val current = players[index]
        val shouldUnmute = current.volumeMuted == true && safeVolume > 0
        if (current.volume == safeVolume && !shouldUnmute) return
        val updated = players.toMutableList()
        updated[index] = current.copy(
            volume = safeVolume,
            volumeMuted = if (shouldUnmute) false else current.volumeMuted
        )
        _availablePlayers.value = updated
        if (_selectedPlayer.value?.playerId == player.playerId) {
            _selectedPlayer.value = updated[index]
        }
        viewModelScope.launch {
            setPlayerVolumeUseCase(player.playerId, safeVolume)
                .onFailure {
                    _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to set volume"))
                }
        }
        if (shouldUnmute) {
            viewModelScope.launch {
                setPlayerMuteUseCase(player.playerId, false)
                    .onFailure {
                        _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to unmute player"))
                    }
            }
        }
    }

    fun setPlayerMute(player: Player, muted: Boolean) {
        val players = _availablePlayers.value
        val index = players.indexOfFirst { it.playerId == player.playerId }
        if (index == -1) return
        val current = players[index]
        if (current.volumeMuted == muted) return
        val updated = players.toMutableList()
        updated[index] = current.copy(volumeMuted = muted)
        _availablePlayers.value = updated
        if (_selectedPlayer.value?.playerId == player.playerId) {
            _selectedPlayer.value = updated[index]
        }
        viewModelScope.launch {
            setPlayerMuteUseCase(player.playerId, muted)
                .onFailure {
                    _events.tryEmit(PlaybackUiEvent.Error(it.message ?: "Failed to update mute"))
                }
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
        val queueState: QueueState,
        val providerBadge: ProviderBadge?
    )

    private data class ProviderMetadata(
        val providerKey: String?,
        val providerDomains: List<String>
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

    private fun hasPreviousTrack(): Boolean {
        val controller = playbackServiceConnection.mediaController.value
        if (controller != null) {
            return controller.previousMediaItemIndex != C.INDEX_UNSET
        }
        val items = queue.value
        if (items.isEmpty()) return false
        val currentItem = displayedMediaItem.value ?: return false
        val currentIndex = items.indexOfFirst { it.mediaId == currentItem.mediaId }
        return currentIndex > 0
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
private const val PREVIOUS_TRACK_THRESHOLD_MS = 3_000L
private const val OPTIMISTIC_PLAYBACK_STATE_TIMEOUT_MS = 2_000L
private const val OPTIMISTIC_REPEAT_TIMEOUT_MS = 3_000L
private const val OPTIMISTIC_SHUFFLE_TIMEOUT_MS = 3_000L
private const val OPTIMISTIC_QUEUE_MOVE_TIMEOUT_MS = 1_500L
private const val PLAYER_REFRESH_THROTTLE_MS = 2_000L
private const val ARTIST_LOOKUP_LIMIT = 50
private const val ALBUM_LOOKUP_LIMIT = 50
private const val MEDIA_ID_PREFIX_ALBUM = "album"
