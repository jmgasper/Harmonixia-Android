package com.harmonixia.android.ui.screens.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.ManagePlaylistTracksUseCase
import com.harmonixia.android.domain.usecase.PlayAlbumUseCase
import com.harmonixia.android.ui.navigation.Screen
import com.harmonixia.android.util.PrefetchScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

sealed class AlbumDetailUiEvent {
    data class ShowMessage(val messageResId: Int) : AlbumDetailUiEvent()
    data object PlaylistCreated : AlbumDetailUiEvent()
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val playAlbumUseCase: PlayAlbumUseCase,
    private val managePlaylistTracksUseCase: ManagePlaylistTracksUseCase,
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

    init {
        loadAlbumTracks()
    }

    fun loadAlbumTracks() {
        viewModelScope.launch {
            if (albumId.isBlank() || provider.isBlank()) {
                _uiState.value = AlbumDetailUiState.Error("Missing album details.")
                return@launch
            }
            _uiState.value = AlbumDetailUiState.Loading
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
