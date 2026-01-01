package com.harmonixia.android.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertFooterItem
import com.harmonixia.android.R
import com.harmonixia.android.data.paging.PlaylistsPagingSource
import com.harmonixia.android.data.remote.WebSocketMessage
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.DeletePlaylistUseCase
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.domain.usecase.RenamePlaylistUseCase
import com.harmonixia.android.util.NetworkConnectivityManager
import com.harmonixia.android.util.PagingStatsTracker
import com.harmonixia.android.util.matchesLocal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed class PlaylistsUiEvent {
    data class ShowMessage(val messageResId: Int) : PlaylistsUiEvent()
    data object PlaylistCreated : PlaylistsUiEvent()
    data class PlaylistDeleted(val playlistId: String) : PlaylistsUiEvent()
    data class PlaylistRenamed(val playlist: Playlist) : PlaylistsUiEvent()
}

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val localMediaRepository: LocalMediaRepository,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager,
    private val pagingStatsTracker: PagingStatsTracker
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = getConnectionStateUseCase()
    val isOfflineMode: StateFlow<Boolean> = combine(
        connectionState,
        networkConnectivityManager.networkAvailabilityFlow
    ) { state, networkAvailable ->
        state !is ConnectionState.Connected || !networkAvailable
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        connectionState.value !is ConnectionState.Connected ||
            !networkConnectivityManager.networkAvailabilityFlow.value
    )

    private val _uiState = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.Loading)
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PlaylistsUiEvent>()
    val events = _events.asSharedFlow()

    private val _isRenaming = MutableStateFlow(false)
    val isRenaming: StateFlow<Boolean> = _isRenaming.asStateFlow()

    private val _renameErrorMessageResId = MutableStateFlow<Int?>(null)
    val renameErrorMessageResId: StateFlow<Int?> = _renameErrorMessageResId.asStateFlow()

    private val pagingConfig = MutableStateFlow(
        PagingConfig(
            pageSize = PlaylistsPagingSource.PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = true,
            initialLoadSize = PlaylistsPagingSource.PAGE_SIZE
        )
    )

    private var pagingSource: PlaylistsPagingSource? = null
    private val playlistTracksCache = ConcurrentHashMap<String, List<Track>>()
    private var wasOffline = true

    private val remotePlaylistsFlow: Flow<PagingData<Playlist>> = pagingConfig
        .flatMapLatest { config ->
            Pager(config) {
                PlaylistsPagingSource(
                    repository,
                    config.pageSize,
                    pagingStatsTracker,
                    isOfflineMode = { isOfflineMode.value }
                ).also {
                    pagingSource = it
                }
            }.flow
        }

    // Local playlists are not exposed yet; keep an empty source tied to local updates.
    private val localPlaylistsFlow: Flow<List<Playlist>> =
        localMediaRepository.getAllTracks().map { emptyList() }

    private val onlinePlaylistsFlow: Flow<PagingData<Playlist>> = localPlaylistsFlow
        .flatMapLatest { localPlaylists ->
            remotePlaylistsFlow.map { pagingData ->
                mergePagingWithLocal(pagingData, localPlaylists)
            }
        }

    private val cachedOnlinePlaylistsFlow: Flow<PagingData<Playlist>> =
        onlinePlaylistsFlow.cachedIn(viewModelScope)

    private val offlinePlaylistsFlow: Flow<PagingData<Playlist>> =
        localMediaRepository.getAllTracks().flatMapLatest { localTracks ->
            cachedOnlinePlaylistsFlow.map { pagingData ->
                pagingData.filter { playlist ->
                    playlistHasLocalTracks(playlist, localTracks)
                }
            }
        }

    private val mergedPlaylistsFlow: Flow<PagingData<Playlist>> = isOfflineMode.flatMapLatest { offline ->
        if (offline) offlinePlaylistsFlow else cachedOnlinePlaylistsFlow
    }

    val playlistsFlow: Flow<PagingData<Playlist>> = mergedPlaylistsFlow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            isOfflineMode.collect { offline ->
                if (wasOffline && !offline) {
                    prefetchPlaylistTracks()
                    pagingSource?.invalidate()
                }
                wasOffline = offline
            }
        }
        viewModelScope.launch {
            combine(connectionState, isOfflineMode) { state, offline ->
                if (offline) {
                    PlaylistsUiState.Success(playlistsFlow)
                } else {
                    when (state) {
                        is ConnectionState.Connected -> PlaylistsUiState.Success(playlistsFlow)
                        is ConnectionState.Connecting -> PlaylistsUiState.Loading
                        is ConnectionState.Disconnected -> PlaylistsUiState.Empty
                        is ConnectionState.Error -> PlaylistsUiState.Error(state.message)
                    }
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
        viewModelScope.launch {
            repository.observeEvents().collect { event ->
                if (isPlaylistEvent(event)) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        pagingSource?.invalidate()
    }

    fun updatePagingConfig(pageSize: Int, prefetchDistance: Int) {
        pagingConfig.value = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance,
            enablePlaceholders = true,
            initialLoadSize = pageSize
        )
        pagingSource?.invalidate()
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.emit(PlaylistsUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = repository.createPlaylist(trimmed)
            result.onSuccess { playlist ->
                _events.emit(PlaylistsUiEvent.ShowMessage(R.string.playlists_create_success))
                _events.emit(PlaylistsUiEvent.PlaylistCreated)
                refresh()
            }.onFailure {
                _events.emit(PlaylistsUiEvent.ShowMessage(R.string.playlists_create_error))
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        if (!playlist.isEditable) {
            viewModelScope.launch {
                _events.emit(PlaylistsUiEvent.ShowMessage(R.string.playlist_delete_error))
            }
            return
        }
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.emit(PlaylistsUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = deletePlaylistUseCase(playlist.itemId)
            result.onSuccess {
                _events.emit(PlaylistsUiEvent.ShowMessage(R.string.playlist_delete_success))
                _events.emit(PlaylistsUiEvent.PlaylistDeleted(playlist.itemId))
                refresh()
            }.onFailure {
                _events.emit(PlaylistsUiEvent.ShowMessage(R.string.playlist_delete_error))
            }
        }
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        val trimmed = newName.trim()
        if (!playlist.isEditable) {
            _renameErrorMessageResId.value = R.string.playlist_rename_error
            return
        }
        if (trimmed.isBlank()) {
            _renameErrorMessageResId.value = R.string.playlist_rename_empty_name
            return
        }
        if (trimmed == playlist.name.trim()) {
            _renameErrorMessageResId.value = R.string.playlist_rename_same_name
            return
        }
        viewModelScope.launch {
            _isRenaming.value = true
            _renameErrorMessageResId.value = null
            if (isOfflineMode.value) {
                _isRenaming.value = false
                _renameErrorMessageResId.value = R.string.offline_feature_unavailable
                _events.emit(PlaylistsUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = renamePlaylistUseCase(playlist.itemId, playlist.provider, trimmed)
            result.onSuccess { renamed ->
                _events.emit(PlaylistsUiEvent.ShowMessage(R.string.playlist_rename_success))
                _events.emit(PlaylistsUiEvent.PlaylistRenamed(renamed))
                _isRenaming.value = false
                refresh()
            }.onFailure {
                _isRenaming.value = false
                _renameErrorMessageResId.value = R.string.playlist_rename_error
                _events.emit(PlaylistsUiEvent.ShowMessage(R.string.playlist_rename_error))
            }
        }
    }

    fun clearRenameError() {
        _renameErrorMessageResId.value = null
    }

    private fun isPlaylistEvent(event: WebSocketMessage.EventMessage): Boolean {
        val name = event.event.lowercase()
        if ("playlist" in name) return true
        val payload = event.data as? JsonObject ?: return false
        val mediaType = payload["media_type"]?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
        if ("playlist" in mediaType) return true
        val uri = payload["uri"]?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
        return "playlist" in uri
    }

    companion object {
        private const val PREFETCH_DISTANCE = 15
    }

    private fun mergePagingWithLocal(
        pagingData: PagingData<Playlist>,
        localPlaylists: List<Playlist>
    ): PagingData<Playlist> {
        if (localPlaylists.isEmpty()) return pagingData
        val localKeys = localPlaylists.map { playlistKey(it) }.toSet()
        val remoteWithoutLocal = pagingData.filter { playlist ->
            playlistKey(playlist) !in localKeys
        }
        return localPlaylists.fold(remoteWithoutLocal) { acc, localPlaylist ->
            acc.insertFooterItem(item = localPlaylist)
        }
    }

    private fun playlistKey(playlist: Playlist): String {
        return "${playlist.provider}:${playlist.itemId}"
    }

    private suspend fun playlistHasLocalTracks(
        playlist: Playlist,
        localTracks: List<Track>
    ): Boolean {
        if (localTracks.isEmpty()) return false
        val playlistTracks = getPlaylistTracksForFilter(playlist)
        if (playlistTracks.isEmpty()) return false
        return playlistTracks.any { playlistTrack ->
            localTracks.any { localTrack ->
                playlistTrack.matchesLocal(localTrack)
            }
        }
    }

    private suspend fun getPlaylistTracksForFilter(playlist: Playlist): List<Track> {
        val key = playlistKey(playlist)
        val cached = playlistTracksCache[key]
        if (cached != null) return cached
        val result = repository.getPlaylistTracks(playlist.itemId, playlist.provider)
        val tracks = result.getOrDefault(emptyList())
        if (result.isSuccess) {
            playlistTracksCache[key] = tracks
        }
        return tracks
    }

    private suspend fun prefetchPlaylistTracks() {
        val playlists = repository.fetchPlaylists(50, 0).getOrDefault(emptyList())
        playlists.forEach { playlist ->
            getPlaylistTracksForFilter(playlist)
        }
    }
}
