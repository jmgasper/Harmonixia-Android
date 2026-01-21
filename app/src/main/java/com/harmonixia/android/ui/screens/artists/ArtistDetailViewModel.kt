package com.harmonixia.android.ui.screens.artists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.PlaybackContext
import com.harmonixia.android.domain.model.PlaybackSource
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import com.harmonixia.android.domain.repository.OfflineLibraryRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.PlayAlbumUseCase
import com.harmonixia.android.ui.navigation.Screen
import com.harmonixia.android.service.playback.PlaybackStateManager
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.NetworkConnectivityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.net.Uri
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

sealed class ArtistDetailUiEvent {
    data class ShowMessage(val messageResId: Int) : ArtistDetailUiEvent()
    data object PlaylistCreated : ArtistDetailUiEvent()
}

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val offlineLibraryRepository: OfflineLibraryRepository,
    private val playAlbumUseCase: PlayAlbumUseCase,
    private val playbackStateManager: PlaybackStateManager,
    private val networkConnectivityManager: NetworkConnectivityManager,
    savedStateHandle: SavedStateHandle,
    val imageQualityManager: ImageQualityManager
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
    val isOfflineMode: StateFlow<Boolean> = networkConnectivityManager.networkAvailabilityFlow
        .map { networkConnectivityManager.isOfflineMode() }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            networkConnectivityManager.isOfflineMode()
        )

    init {
        viewModelScope.launch {
            isOfflineMode.collect {
                loadArtistAlbums()
            }
        }
    }

    fun loadArtistAlbums() {
        viewModelScope.launch {
            if (artistId.isBlank() || provider.isBlank()) {
                _uiState.value = ArtistDetailUiState.Error("Missing artist details.")
                return@launch
            }
            _uiState.value = ArtistDetailUiState.Loading
            val useOfflineLibrary = isOfflineMode.value || provider == OFFLINE_PROVIDER
            if (useOfflineLibrary) {
                val resolvedName = resolveOfflineArtistName()
                if (resolvedName.isBlank()) {
                    _uiState.value = ArtistDetailUiState.Error("Artist not found.")
                    return@launch
                }
                val albums = offlineLibraryRepository
                    .getDownloadedAlbumsByArtist(resolvedName)
                    .first()
                val sortedAlbums = sortAlbumsByYear(albums)
                val offlineArtist = Artist(
                    itemId = Uri.encode(resolvedName),
                    provider = OFFLINE_PROVIDER,
                    uri = "offline:artist:${Uri.encode(resolvedName)}",
                    name = resolvedName,
                    sortName = resolvedName.lowercase(),
                    imageUrl = _artist.value?.imageUrl
                )
                _artist.value = offlineArtist
                _uiState.value = if (sortedAlbums.isEmpty()) {
                    ArtistDetailUiState.Empty
                } else {
                    ArtistDetailUiState.Success(offlineArtist, sortedAlbums, emptyList())
                }
                return@launch
            }
            supervisorScope {
                val artistDeferred = async { repository.getArtist(artistId, provider) }
                val libraryAlbumsDeferred = async {
                    repository.getArtistAlbums(artistId, provider, inLibraryOnly = true)
                }
                val allAlbumsDeferred = async {
                    repository.getArtistAlbums(artistId, provider, inLibraryOnly = false)
                }
                val artistsResult = artistDeferred.await()
                val libraryAlbumsResult = libraryAlbumsDeferred.await()
                val allAlbumsResult = allAlbumsDeferred.await()
                val error = artistsResult.exceptionOrNull()
                    ?: libraryAlbumsResult.exceptionOrNull()
                    ?: allAlbumsResult.exceptionOrNull()
                if (error != null) {
                    _uiState.value = ArtistDetailUiState.Error(error.message ?: "Unknown error")
                    return@supervisorScope
                }
                val selectedArtist = artistsResult.getOrNull()
                _artist.value = selectedArtist
                if (selectedArtist == null) {
                    _uiState.value = ArtistDetailUiState.Error("Artist not found.")
                    return@supervisorScope
                }
                val libraryAlbums = sortAlbumsByYear(libraryAlbumsResult.getOrDefault(emptyList()))
                val allAlbums = sortAlbumsByYear(allAlbumsResult.getOrDefault(emptyList()))
                _uiState.value = if (libraryAlbums.isEmpty() && allAlbums.isEmpty()) {
                    ArtistDetailUiState.Empty
                } else {
                    ArtistDetailUiState.Success(selectedArtist, libraryAlbums, allAlbums)
                }
            }
        }
    }

    fun playAlbum(album: Album) {
        viewModelScope.launch {
            val albumTitle = album.name.trim().takeIf { it.isNotBlank() }
            playbackStateManager.setPlaybackContext(
                PlaybackContext(source = PlaybackSource.ALBUM, title = albumTitle)
            )
            playAlbumUseCase(
                albumId = album.itemId,
                provider = album.provider,
                albumUri = album.uri
            )
        }
    }

    fun refreshPlaylists() {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(ArtistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
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
            if (isOfflineMode.value) {
                _events.tryEmit(ArtistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
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
            if (isOfflineMode.value) {
                _events.emit(ArtistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
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

    private fun resolveOfflineArtistName(): String {
        val existing = _artist.value?.name
        if (!existing.isNullOrBlank()) return existing
        return if (provider == OFFLINE_PROVIDER) {
            Uri.decode(artistId)
        } else {
            artistId
        }
    }

    companion object {
        private const val PLAYLIST_LIST_LIMIT = 200
    }
}

private fun sortAlbumsByYear(albums: List<Album>): List<Album> {
    if (albums.size < 2) return albums
    return albums.sortedWith(AlbumYearDescendingComparator)
}

private fun albumYearSortKey(album: Album): Int {
    val year = album.year ?: 0
    return if (year > 0) year else Int.MIN_VALUE
}

private fun albumNameSortKey(album: Album): String {
    return album.name.trim().lowercase()
}

private val AlbumYearDescendingComparator = compareByDescending<Album> { albumYearSortKey(it) }
    .thenBy { albumNameSortKey(it) }
