package com.harmonixia.android.ui.screens.artists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.PlayAlbumUseCase
import com.harmonixia.android.ui.navigation.Screen
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

sealed class ArtistDetailUiEvent {
    data class ShowMessage(val messageResId: Int) : ArtistDetailUiEvent()
    data object PlaylistCreated : ArtistDetailUiEvent()
}

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val playAlbumUseCase: PlayAlbumUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val artistId: String =
        savedStateHandle.get<String>(Screen.ArtistDetail.ARG_ARTIST_ID).orEmpty()
    private val provider: String =
        savedStateHandle.get<String>(Screen.ArtistDetail.ARG_PROVIDER).orEmpty()

    private val _uiState = MutableStateFlow<ArtistDetailUiState>(ArtistDetailUiState.Loading)
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _events = MutableSharedFlow<ArtistDetailUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        loadArtistAlbums()
    }

    fun loadArtistAlbums() {
        viewModelScope.launch {
            if (artistId.isBlank() || provider.isBlank()) {
                _uiState.value = ArtistDetailUiState.Error("Missing artist details.")
                return@launch
            }
            _uiState.value = ArtistDetailUiState.Loading
            supervisorScope {
                val artistsDeferred = async { repository.fetchArtists(ARTIST_LIST_LIMIT, 0) }
                val albumsDeferred = async { fetchAllAlbums() }
                val artistsResult = artistsDeferred.await()
                val albumsResult = albumsDeferred.await()
                val error = artistsResult.exceptionOrNull() ?: albumsResult.exceptionOrNull()
                if (error != null) {
                    _uiState.value = ArtistDetailUiState.Error(error.message ?: "Unknown error")
                    return@supervisorScope
                }
                val artists = artistsResult.getOrDefault(emptyList())
                val selectedArtist = artists.firstOrNull { artist ->
                    artist.itemId == artistId && artist.provider == provider
                }
                _artist.value = selectedArtist
                if (selectedArtist == null) {
                    _uiState.value = ArtistDetailUiState.Error("Artist not found.")
                    return@supervisorScope
                }
                val albums = albumsResult.getOrDefault(emptyList())
                val filteredAlbums = filterAlbumsForArtist(albums, selectedArtist)
                _uiState.value = if (filteredAlbums.isEmpty()) {
                    ArtistDetailUiState.Empty
                } else {
                    ArtistDetailUiState.Success(selectedArtist, filteredAlbums)
                }
            }
        }
    }

    fun playAlbum(album: Album) {
        viewModelScope.launch {
            playAlbumUseCase(album.itemId, album.provider)
        }
    }

    fun refreshPlaylists() {
        viewModelScope.launch {
            val result = repository.fetchPlaylists(PLAYLIST_LIST_LIMIT, 0)
            result.onSuccess { playlists ->
                _playlists.value = playlists
            }.onFailure {
                _events.tryEmit(ArtistDetailUiEvent.ShowMessage(R.string.playlists_error))
            }
        }
    }

    fun addAlbumToPlaylist(album: Album, targetPlaylist: Playlist) {
        if (!targetPlaylist.isEditable) {
            _events.tryEmit(ArtistDetailUiEvent.ShowMessage(R.string.track_add_error))
            return
        }
        viewModelScope.launch {
            val tracksResult = repository.getAlbumTracks(album.itemId, album.provider)
            val tracks = tracksResult.getOrDefault(emptyList())
            val uris = tracks.mapNotNull { track -> track.uri.takeIf { it.isNotBlank() } }
            if (tracksResult.isFailure || uris.isEmpty()) {
                _events.tryEmit(ArtistDetailUiEvent.ShowMessage(R.string.track_add_error))
                return@launch
            }
            val result = repository.addTracksToPlaylist(targetPlaylist.itemId, uris)
            result.onSuccess {
                _events.tryEmit(ArtistDetailUiEvent.ShowMessage(R.string.track_add_success))
            }.onFailure {
                _events.tryEmit(ArtistDetailUiEvent.ShowMessage(R.string.track_add_error))
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
                _events.emit(ArtistDetailUiEvent.ShowMessage(R.string.playlists_create_success))
                _events.emit(ArtistDetailUiEvent.PlaylistCreated)
                refreshPlaylists()
            }.onFailure {
                _events.emit(ArtistDetailUiEvent.ShowMessage(R.string.playlists_create_error))
            }
        }
    }

    private fun filterAlbumsForArtist(albums: List<Album>, artist: Artist): List<Album> {
        val targetName = normalizeName(artist.name)
        if (targetName.isBlank()) return emptyList()
        return albums.filter { album ->
            albumArtistNames(album).any { name ->
                normalizeName(name) == targetName
            }
        }
    }

    private fun albumArtistNames(album: Album): List<String> {
        return album.artists
    }

    private fun normalizeName(name: String?): String {
        return name?.trim()?.lowercase().orEmpty()
    }

    private suspend fun fetchAllAlbums(): Result<List<Album>> {
        return runCatching {
            val albums = mutableListOf<Album>()
            var offset = 0
            while (true) {
                val page = repository.fetchAlbums(ALBUM_LIST_LIMIT, offset).getOrThrow()
                if (page.isEmpty()) break
                albums.addAll(page)
                if (page.size < ALBUM_LIST_LIMIT) break
                offset += ALBUM_LIST_LIMIT
            }
            albums
        }
    }

    companion object {
        private const val ARTIST_LIST_LIMIT = 200
        private const val ALBUM_LIST_LIMIT = 200
        private const val PLAYLIST_LIST_LIMIT = 200
    }
}
