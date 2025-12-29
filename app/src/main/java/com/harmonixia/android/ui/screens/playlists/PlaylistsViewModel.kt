package com.harmonixia.android.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.harmonixia.android.R
import com.harmonixia.android.data.paging.PlaylistsPagingSource
import com.harmonixia.android.data.remote.WebSocketMessage
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.DeletePlaylistUseCase
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.domain.usecase.RenamePlaylistUseCase
import com.harmonixia.android.util.NetworkConnectivityManager
import com.harmonixia.android.util.PagingStatsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
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
    private val downloadRepository: DownloadRepository,
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

    private val onlinePlaylistsFlow: Flow<PagingData<Playlist>> = pagingConfig
        .flatMapLatest { config ->
            Pager(config) {
                PlaylistsPagingSource(repository, config.pageSize, pagingStatsTracker).also {
                    pagingSource = it
                }
            }.flow
        }

    private val offlinePlaylistsFlow: Flow<PagingData<Playlist>> =
        downloadRepository.getDownloadedPlaylists().map { playlists ->
            PagingData.from(playlists)
        }

    val playlistsFlow: Flow<PagingData<Playlist>> = isOfflineMode
        .flatMapLatest { offline ->
            if (offline) offlinePlaylistsFlow else onlinePlaylistsFlow
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            isOfflineMode.collect { offline ->
                if (offline) {
                    pagingSource?.invalidate()
                }
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
}
