package com.harmonixia.android.ui.screens.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.DownloadProgress
import com.harmonixia.android.domain.model.DownloadStatus
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.model.downloadId
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.ManagePlaylistTracksUseCase
import com.harmonixia.android.domain.usecase.PlayAlbumUseCase
import com.harmonixia.android.ui.navigation.Screen
import com.harmonixia.android.util.NetworkConnectivityManager
import com.harmonixia.android.util.PrefetchScheduler
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

sealed class AlbumDetailUiEvent {
    data class ShowMessage(val messageResId: Int) : AlbumDetailUiEvent()
    data object PlaylistCreated : AlbumDetailUiEvent()
    data object ShowConfirmRemoveDownload : AlbumDetailUiEvent()
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val downloadRepository: DownloadRepository,
    private val playAlbumUseCase: PlayAlbumUseCase,
    private val managePlaylistTracksUseCase: ManagePlaylistTracksUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager,
    private val prefetchScheduler: PrefetchScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val albumId: String =
        savedStateHandle.get<String>(Screen.AlbumDetail.ARG_ALBUM_ID).orEmpty()
    private val provider: String =
        savedStateHandle.get<String>(Screen.AlbumDetail.ARG_PROVIDER).orEmpty()

    private val _uiState = MutableStateFlow<AlbumDetailUiState>(AlbumDetailUiState.Loading)
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _events = MutableSharedFlow<AlbumDetailUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var tracks: List<Track> = emptyList()
    private val albumTracksFlow: Flow<List<Track>> = uiState.map { state ->
        if (state is AlbumDetailUiState.Success) state.tracks else emptyList()
    }
    val isOfflineMode: StateFlow<Boolean> = networkConnectivityManager.networkAvailabilityFlow
        .map { networkConnectivityManager.isOfflineMode() }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            networkConnectivityManager.isOfflineMode()
        )
    val albumDownloadStatus: StateFlow<DownloadStatus?> = album
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
    val albumDownloadProgress: StateFlow<DownloadProgress?> = album
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
    val isAlbumDownloaded: StateFlow<Boolean> = combine(
        albumTracksFlow,
        downloadRepository.getDownloadedTracks()
    ) { albumTracks, downloadedTracks ->
        if (albumTracks.isEmpty()) {
            false
        } else {
            val downloadedIds = downloadedTracks.map { it.downloadId }.toSet()
            albumTracks.all { track -> track.downloadId in downloadedIds }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            isOfflineMode.collect {
                loadAlbumTracks()
            }
        }
    }

    fun loadAlbumTracks() {
        viewModelScope.launch {
            if (albumId.isBlank() || provider.isBlank()) {
                _uiState.value = AlbumDetailUiState.Error("Missing album details.")
                return@launch
            }
            _uiState.value = AlbumDetailUiState.Loading
            if (isOfflineMode.value) {
                val downloadId = "$albumId-$provider"
                val isDownloaded = downloadRepository.isAlbumDownloaded(downloadId)
                val downloadedAlbum = downloadRepository.getDownloadedAlbums()
                    .first()
                    .firstOrNull { album -> album.itemId == albumId && album.provider == provider }
                if (!isDownloaded || downloadedAlbum == null) {
                    _album.value = downloadedAlbum
                    tracks = emptyList()
                    _uiState.value = AlbumDetailUiState.Empty
                    return@launch
                }
                val downloadedTracks = downloadRepository.getAllCompletedDownloads()
                    .first()
                    .filter { track -> track.albumId == downloadId }
                    .map { track -> track.track }
                _album.value = downloadedAlbum
                tracks = downloadedTracks
                _uiState.value = if (downloadedTracks.isEmpty()) {
                    AlbumDetailUiState.Empty
                } else {
                    AlbumDetailUiState.Success(downloadedAlbum, downloadedTracks)
                }
                return@launch
            }
            supervisorScope {
                val albumDeferred = async { repository.getAlbum(albumId, provider) }
                val tracksDeferred = async { repository.getAlbumTracks(albumId, provider) }
                val albumResult = albumDeferred.await()
                val tracksResult = tracksDeferred.await()
                val error = albumResult.exceptionOrNull() ?: tracksResult.exceptionOrNull()
                if (error != null) {
                    _uiState.value = AlbumDetailUiState.Error(error.message ?: "Unknown error")
                    return@supervisorScope
                }
                val loadedAlbum = albumResult.getOrThrow()
                val loadedTracks = tracksResult.getOrDefault(emptyList())
                _album.value = loadedAlbum
                tracks = loadedTracks
                prefetchArtistData(loadedAlbum)
                _uiState.value = if (loadedTracks.isEmpty()) {
                    AlbumDetailUiState.Empty
                } else {
                    AlbumDetailUiState.Success(loadedAlbum, loadedTracks)
                }
            }
        }
    }

    fun playAlbum(startIndex: Int = 0) {
        if (albumId.isBlank() || provider.isBlank()) return
        viewModelScope.launch {
            if (isOfflineMode.value) {
                val downloadId = "$albumId-$provider"
                val isDownloaded = downloadRepository.isAlbumDownloaded(downloadId)
                if (!isDownloaded) {
                    _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.no_downloaded_content))
                    return@launch
                }
            }
            playAlbumUseCase(albumId, provider, startIndex)
        }
    }

    fun playTrack(track: Track) {
        if (tracks.isEmpty()) return
        val index = tracks.indexOfFirst { it.itemId == track.itemId }
        if (index >= 0) {
            playAlbum(index)
        } else {
            playAlbum(0)
        }
    }

    fun refreshPlaylists() {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = repository.fetchPlaylists(PLAYLIST_LIST_LIMIT, 0)
            result.onSuccess { playlists ->
                _playlists.value = playlists
            }.onFailure {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.playlists_error))
            }
        }
    }

    fun addTrackToPlaylist(targetPlaylist: Playlist, track: Track) {
        if (!targetPlaylist.isEditable) {
            _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.track_add_error))
            return
        }
        val uri = track.uri
        if (uri.isBlank()) {
            _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.track_add_error))
            return
        }
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = managePlaylistTracksUseCase.addTrackToPlaylist(
                playlistId = targetPlaylist.itemId,
                trackUri = uri,
                isEditable = targetPlaylist.isEditable
            )
            result.onSuccess {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.track_add_success))
            }.onFailure {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.track_add_error))
            }
        }
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.emit(AlbumDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = repository.createPlaylist(trimmed)
            result.onSuccess { playlist ->
                _playlists.value = listOf(playlist) + _playlists.value
                _events.emit(AlbumDetailUiEvent.ShowMessage(R.string.playlists_create_success))
                _events.emit(AlbumDetailUiEvent.PlaylistCreated)
                refreshPlaylists()
            }.onFailure {
                _events.emit(AlbumDetailUiEvent.ShowMessage(R.string.playlists_create_error))
            }
        }
    }

    fun downloadAlbum() {
        val currentAlbum = album.value ?: return
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = downloadRepository.queueAlbumDownload(currentAlbum, tracks)
            result.onSuccess {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.album_download_success))
            }.onFailure {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.album_download_error))
            }
        }
    }

    fun removeAlbumDownload() {
        val downloadId = album.value?.downloadId ?: "$albumId-$provider"
        if (downloadId.isBlank()) return
        viewModelScope.launch {
            val result = downloadRepository.deleteAlbumDownload(downloadId)
            result.onSuccess {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.album_download_removed))
            }.onFailure {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.album_download_remove_error))
            }
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val albumDownloadId = album.value?.downloadId ?: "$albumId-$provider"
            val result = downloadRepository.queueTrackDownload(track, albumDownloadId, emptyList())
            result.onSuccess {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.track_download_success))
            }.onFailure {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.track_download_error))
            }
        }
    }

    fun getTrackDownloadStatus(trackId: String): Flow<DownloadStatus?> {
        return downloadRepository.getDownloadStatus(trackId)
    }

    fun getTrackDownloadProgress(trackId: String): Flow<DownloadProgress?> {
        return downloadRepository.getDownloadProgress(trackId)
    }

    private fun prefetchArtistData(album: Album) {
        val artistNames = album.artists.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        if (artistNames.isEmpty()) return
        viewModelScope.launch {
            repository.fetchArtists(ARTIST_PREFETCH_LIMIT, 0)
            repository.fetchAlbums(ALBUM_PREFETCH_LIMIT, 0)
        }
        prefetchScheduler.scheduleArtistPrefetch(artistNames)
    }

    private companion object {
        private const val PLAYLIST_LIST_LIMIT = 200
        private const val ARTIST_PREFETCH_LIMIT = 50
        private const val ALBUM_PREFETCH_LIMIT = 50
    }
}
