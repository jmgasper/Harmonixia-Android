package com.harmonixia.android.ui.screens.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.data.remote.WebSocketMessage
import com.harmonixia.android.domain.model.DownloadProgress
import com.harmonixia.android.domain.model.DownloadStatus
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.model.downloadId
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.DeletePlaylistUseCase
import com.harmonixia.android.domain.usecase.ManagePlaylistTracksUseCase
import com.harmonixia.android.domain.usecase.PlayPlaylistUseCase
import com.harmonixia.android.domain.usecase.RenamePlaylistUseCase
import com.harmonixia.android.ui.navigation.Screen
import com.harmonixia.android.util.NetworkConnectivityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed class PlaylistDetailUiEvent {
    data class ShowMessage(val messageResId: Int) : PlaylistDetailUiEvent()
    data object PlaylistCreated : PlaylistDetailUiEvent()
    data object PlaylistDeleted : PlaylistDetailUiEvent()
    data class PlaylistRenamed(val playlist: Playlist) : PlaylistDetailUiEvent()
    data object ShowConfirmRemoveDownload : PlaylistDetailUiEvent()
}

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val downloadRepository: DownloadRepository,
    private val playPlaylistUseCase: PlayPlaylistUseCase,
    private val managePlaylistTracksUseCase: ManagePlaylistTracksUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val playlistId: String =
        savedStateHandle.get<String>(Screen.PlaylistDetail.ARG_PLAYLIST_ID).orEmpty()
    private val provider: String =
        savedStateHandle.get<String>(Screen.PlaylistDetail.ARG_PROVIDER).orEmpty()

    private val _uiState = MutableStateFlow<PlaylistDetailUiState>(PlaylistDetailUiState.Loading)
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _isRenaming = MutableStateFlow(false)
    val isRenaming: StateFlow<Boolean> = _isRenaming.asStateFlow()

    private val _renameErrorMessageResId = MutableStateFlow<Int?>(null)
    val renameErrorMessageResId: StateFlow<Int?> = _renameErrorMessageResId.asStateFlow()

    private val _events = MutableSharedFlow<PlaylistDetailUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var tracks: List<Track> = emptyList()
    private val playlistTracksFlow: Flow<List<Track>> = uiState.map { state ->
        if (state is PlaylistDetailUiState.Success) state.tracks else emptyList()
    }
    val isOfflineMode: StateFlow<Boolean> = networkConnectivityManager.networkAvailabilityFlow
        .map { networkConnectivityManager.isOfflineMode() }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            networkConnectivityManager.isOfflineMode()
        )
    val playlistDownloadStatus: StateFlow<DownloadStatus?> = playlist
        .map { it?.downloadId }
        .distinctUntilChanged()
        .flatMapLatest { downloadId ->
            if (downloadId.isNullOrBlank()) {
                flowOf(null)
            } else {
                downloadRepository.getDownloadStatus(downloadId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val playlistDownloadProgress: StateFlow<DownloadProgress?> = playlist
        .map { it?.downloadId }
        .distinctUntilChanged()
        .flatMapLatest { downloadId ->
            if (downloadId.isNullOrBlank()) {
                flowOf(null)
            } else {
                downloadRepository.getDownloadProgress(downloadId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val isPlaylistDownloaded: StateFlow<Boolean> = combine(
        playlistTracksFlow,
        downloadRepository.getDownloadedTracks()
    ) { playlistTracks, downloadedTracks ->
        if (playlistTracks.isEmpty()) {
            false
        } else {
            val downloadedIds = downloadedTracks.map { it.downloadId }.toSet()
            playlistTracks.all { track -> track.downloadId in downloadedIds }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            isOfflineMode.collect {
                loadPlaylistTracks()
            }
        }
        viewModelScope.launch {
            repository.observeEvents().collect { event ->
                if (isPlaylistEvent(event)) {
                    refreshPlaylist()
                    refreshPlaylists()
                }
            }
        }
    }

    fun loadPlaylistTracks() {
        viewModelScope.launch {
            if (playlistId.isBlank() || provider.isBlank()) {
                _uiState.value = PlaylistDetailUiState.Error("Missing playlist details.")
                return@launch
            }
            _uiState.value = PlaylistDetailUiState.Loading
            if (isOfflineMode.value) {
                val downloadId = "$playlistId-$provider"
                val downloadedPlaylists = downloadRepository.getDownloadedPlaylists().first()
                val selected = downloadedPlaylists.firstOrNull { playlist ->
                    playlist.itemId == playlistId && playlist.provider == provider
                }
                _playlists.value = downloadedPlaylists
                _playlist.value = selected
                if (selected == null) {
                    tracks = emptyList()
                    _uiState.value = PlaylistDetailUiState.Empty
                    return@launch
                }
                val downloadedTracks = downloadRepository.getAllCompletedDownloads()
                    .first()
                    .filter { track -> downloadId in track.playlistIds }
                    .map { track -> track.track }
                tracks = downloadedTracks
                _uiState.value = if (downloadedTracks.isEmpty()) {
                    PlaylistDetailUiState.Empty
                } else {
                    PlaylistDetailUiState.Success(downloadedTracks)
                }
                return@launch
            }
            supervisorScope {
                val playlistsDeferred = async { repository.fetchPlaylists(PLAYLIST_LIST_LIMIT, 0) }
                val tracksDeferred = async { repository.getPlaylistTracks(playlistId, provider) }
                val playlistsResult = playlistsDeferred.await()
                val tracksResult = tracksDeferred.await()
                val error = playlistsResult.exceptionOrNull() ?: tracksResult.exceptionOrNull()
                if (error != null) {
                    _uiState.value = PlaylistDetailUiState.Error(error.message ?: "Unknown error")
                    return@supervisorScope
                }
                val playlists = playlistsResult.getOrDefault(emptyList())
                _playlists.value = playlists
                val selected = playlists.firstOrNull { playlist ->
                    playlist.itemId == playlistId && playlist.provider == provider
                }
                _playlist.value = selected
                if (selected == null) {
                    _uiState.value = PlaylistDetailUiState.Error("Playlist not found.")
                    return@supervisorScope
                }
                val loadedTracks = tracksResult.getOrDefault(emptyList())
                tracks = loadedTracks
                _uiState.value = if (loadedTracks.isEmpty()) {
                    PlaylistDetailUiState.Empty
                } else {
                    PlaylistDetailUiState.Success(loadedTracks)
                }
            }
        }
    }

    fun refreshPlaylists() {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = repository.fetchPlaylists(PLAYLIST_LIST_LIMIT, 0)
            result.onSuccess { playlists ->
                _playlists.value = playlists
            }.onFailure {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.playlists_error))
            }
        }
    }

    fun refreshPlaylist(showLoading: Boolean = false) {
        if (playlistId.isBlank() || provider.isBlank()) return
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            if (showLoading) {
                _uiState.value = PlaylistDetailUiState.Loading
            }
            supervisorScope {
                val playlistDeferred = async { repository.getPlaylist(playlistId, provider) }
                val tracksDeferred = async { repository.getPlaylistTracks(playlistId, provider) }
                val playlistResult = playlistDeferred.await()
                val tracksResult = tracksDeferred.await()
                val error = playlistResult.exceptionOrNull() ?: tracksResult.exceptionOrNull()
                if (error != null) {
                    _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.playlists_error))
                    return@supervisorScope
                }
                val updatedPlaylist = playlistResult.getOrNull()
                if (updatedPlaylist != null) {
                    _playlist.value = updatedPlaylist
                }
                val loadedTracks = tracksResult.getOrDefault(emptyList())
                tracks = loadedTracks
                _uiState.value = if (loadedTracks.isEmpty()) {
                    PlaylistDetailUiState.Empty
                } else {
                    PlaylistDetailUiState.Success(loadedTracks)
                }
            }
        }
    }

    fun playPlaylist(startIndex: Int = 0) {
        if (playlistId.isBlank() || provider.isBlank()) return
        viewModelScope.launch {
            if (isOfflineMode.value) {
                val downloadId = "$playlistId-$provider"
                val isDownloaded = downloadRepository.isPlaylistDownloaded(downloadId)
                if (!isDownloaded) {
                    _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.no_downloaded_content))
                    return@launch
                }
            }
            playPlaylistUseCase(playlistId, provider, startIndex)
        }
    }

    fun playTrack(track: Track) {
        if (tracks.isEmpty()) return
        val index = tracks.indexOfFirst { it.itemId == track.itemId }
        if (index >= 0) {
            playPlaylist(index)
        } else {
            playPlaylist(0)
        }
    }

    fun addTrackToPlaylist(
        targetPlaylistId: String,
        isEditable: Boolean,
        track: Track
    ) {
        if (!isEditable) {
            _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_add_error))
            return
        }
        val uri = track.uri
        if (uri.isBlank()) {
            _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_add_error))
            return
        }
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = managePlaylistTracksUseCase.addTrackToPlaylist(
                playlistId = targetPlaylistId,
                trackUri = uri,
                isEditable = isEditable
            )
            result.onSuccess {
                if (targetPlaylistId == playlistId) {
                    val updated = tracks + track
                    tracks = updated
                    _uiState.value = PlaylistDetailUiState.Success(updated)
                    refreshPlaylist()
                }
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_add_success))
            }.onFailure {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_add_error))
            }
        }
    }

    fun removeTrackFromPlaylist(track: Track, position: Int) {
        val playlist = _playlist.value ?: return
        if (!playlist.isEditable) return
        val resolvedPosition = resolveTrackPosition(track, position)
        if (resolvedPosition < 0) return
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = managePlaylistTracksUseCase.removeTrackFromPlaylist(playlistId, resolvedPosition)
            result.onSuccess {
                val updated = tracks.toMutableList()
                if (resolvedPosition in updated.indices) {
                    updated.removeAt(resolvedPosition)
                }
                tracks = updated
                _uiState.value = if (updated.isEmpty()) {
                    PlaylistDetailUiState.Empty
                } else {
                    PlaylistDetailUiState.Success(updated)
                }
                refreshPlaylist()
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_remove_success))
            }.onFailure {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_remove_error))
            }
        }
    }

    fun deletePlaylist() {
        val playlist = _playlist.value ?: return
        if (!playlist.isEditable) {
            _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_delete_error))
            return
        }
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = deletePlaylistUseCase(playlist.itemId)
            result.onSuccess {
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_delete_success))
                _events.emit(PlaylistDetailUiEvent.PlaylistDeleted)
            }.onFailure {
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_delete_error))
            }
        }
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = repository.createPlaylist(trimmed)
            result.onSuccess { playlist ->
                _playlists.value = listOf(playlist) + _playlists.value
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlists_create_success))
                _events.emit(PlaylistDetailUiEvent.PlaylistCreated)
                refreshPlaylists()
            }.onFailure {
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlists_create_error))
            }
        }
    }

    fun renamePlaylist(newName: String) {
        val playlist = _playlist.value ?: return
        if (!playlist.isEditable) {
            _renameErrorMessageResId.value = R.string.playlist_rename_error
            return
        }
        val trimmed = newName.trim()
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
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = renamePlaylistUseCase(playlist.itemId, playlist.provider, trimmed)
            result.onSuccess { newPlaylist ->
                _isRenaming.value = false
                _playlist.value = newPlaylist
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_rename_success))
                _events.emit(PlaylistDetailUiEvent.PlaylistRenamed(newPlaylist))
                refreshPlaylists()
            }.onFailure {
                _isRenaming.value = false
                _renameErrorMessageResId.value = R.string.playlist_rename_error
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_rename_error))
            }
        }
    }

    fun clearRenameError() {
        _renameErrorMessageResId.value = null
    }

    fun downloadPlaylist() {
        val currentPlaylist = playlist.value ?: return
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = downloadRepository.queuePlaylistDownload(currentPlaylist, tracks)
            result.onSuccess {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_download_success))
            }.onFailure {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_download_error))
            }
        }
    }

    fun removePlaylistDownload() {
        val downloadId = playlist.value?.downloadId ?: "$playlistId-$provider"
        if (downloadId.isBlank()) return
        viewModelScope.launch {
            val result = downloadRepository.deletePlaylistDownload(downloadId)
            result.onSuccess {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_download_removed))
            }.onFailure {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_download_remove_error))
            }
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val playlistDownloadId = playlist.value?.downloadId ?: "$playlistId-$provider"
            val result = downloadRepository.queueTrackDownload(track, null, listOf(playlistDownloadId))
            result.onSuccess {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_download_success))
            }.onFailure {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_download_error))
            }
        }
    }

    fun getTrackDownloadStatus(trackId: String): Flow<DownloadStatus?> {
        return downloadRepository.getDownloadStatus(trackId)
    }

    fun getTrackDownloadProgress(trackId: String): Flow<DownloadProgress?> {
        return downloadRepository.getDownloadProgress(trackId)
    }

    private fun resolveTrackPosition(track: Track, position: Int): Int {
        return if (position in tracks.indices && tracks[position].itemId == track.itemId) {
            position
        } else {
            tracks.indexOfFirst { it.itemId == track.itemId }
        }
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
        private const val PLAYLIST_LIST_LIMIT = 200
    }
}
