package com.harmonixia.android.ui.screens.albums

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import com.harmonixia.android.domain.usecase.ManagePlaylistTracksUseCase
import com.harmonixia.android.domain.usecase.PlayAlbumUseCase
import com.harmonixia.android.domain.usecase.PlayLocalTracksUseCase
import com.harmonixia.android.ui.navigation.Screen
import com.harmonixia.android.util.isLocal
import com.harmonixia.android.util.mergeWithLocal
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

sealed class AlbumDetailUiEvent {
    data class ShowMessage(val messageResId: Int) : AlbumDetailUiEvent()
    data object PlaylistCreated : AlbumDetailUiEvent()
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val localMediaRepository: LocalMediaRepository,
    private val playAlbumUseCase: PlayAlbumUseCase,
    private val playLocalTracksUseCase: PlayLocalTracksUseCase,
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
            if (isOfflineMode.value || provider == OFFLINE_PROVIDER) {
                val localAlbum = resolveLocalAlbum()
                if (localAlbum == null) {
                    _uiState.value = AlbumDetailUiState.Error("Album not found.")
                    return@launch
                }
                val localTracks = loadLocalTracks(localAlbum)
                _album.value = localAlbum
                tracks = localTracks
                _uiState.value = if (localTracks.isEmpty()) {
                    AlbumDetailUiState.Empty
                } else {
                    AlbumDetailUiState.Success(localAlbum, localTracks)
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
                val localTracks = loadLocalTracks(loadedAlbum)
                val mergedTracks = loadedTracks.mergeWithLocal(localTracks)
                val resolvedTracks = if (isOfflineMode.value) {
                    mergedTracks.filter { it.isLocal }
                } else {
                    mergedTracks
                }
                _album.value = loadedAlbum
                tracks = resolvedTracks
                prefetchArtistData(loadedAlbum)
                _uiState.value = if (resolvedTracks.isEmpty()) {
                    AlbumDetailUiState.Empty
                } else {
                    AlbumDetailUiState.Success(loadedAlbum, resolvedTracks)
                }
            }
        }
    }

    fun playAlbum(startIndex: Int = 0, forceStartIndex: Boolean = false) {
        if (albumId.isBlank() || provider.isBlank()) return
        viewModelScope.launch {
            if (isOfflineMode.value || provider == OFFLINE_PROVIDER) {
                val localTracks = tracks.filter { it.isLocal }
                if (localTracks.isNotEmpty()) {
                    val localIndex = resolveLocalStartIndex(startIndex, localTracks)
                    playLocalTracksUseCase(localTracks, localIndex)
                }
            } else {
                playAlbumUseCase(albumId, provider, startIndex, forceStartIndex)
            }
        }
    }

    fun playTrack(track: Track) {
        if (tracks.isEmpty()) return
        val index = tracks.indexOfFirst { it.itemId == track.itemId }
        if (index >= 0) {
            playAlbum(index, forceStartIndex = true)
        } else {
            playAlbum(0, forceStartIndex = true)
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

    fun addTrackToFavorites(track: Track) {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = repository.addToFavorites(track.itemId, track.provider, "track")
            result.onSuccess {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.track_favorite_add_success))
                updateTrackFavoriteState(track, isFavorite = true)
            }.onFailure {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.track_favorite_add_error))
            }
        }
    }

    fun removeTrackFromFavorites(track: Track) {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = repository.removeFromFavorites(track.itemId, track.provider, "track")
            result.onSuccess {
                _events.tryEmit(
                    AlbumDetailUiEvent.ShowMessage(R.string.track_favorite_remove_success)
                )
                updateTrackFavoriteState(track, isFavorite = false)
            }.onFailure {
                _events.tryEmit(
                    AlbumDetailUiEvent.ShowMessage(R.string.track_favorite_remove_error)
                )
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

    private fun prefetchArtistData(album: Album) {
        val artistNames = album.artists.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        if (artistNames.isEmpty()) return
        viewModelScope.launch {
            repository.fetchArtists(ARTIST_PREFETCH_LIMIT, 0)
            repository.fetchAlbums(ALBUM_PREFETCH_LIMIT, 0)
        }
        prefetchScheduler.scheduleArtistPrefetch(artistNames)
    }

    private fun updateTrackFavoriteState(track: Track, isFavorite: Boolean) {
        val currentAlbum = (uiState.value as? AlbumDetailUiState.Success)?.album
            ?: album.value
            ?: return
        if (tracks.isEmpty()) return
        val updatedTracks = tracks.map { existing ->
            if (existing.itemId == track.itemId && existing.provider == track.provider) {
                existing.copy(isFavorite = isFavorite)
            } else {
                existing
            }
        }
        if (updatedTracks == tracks) return
        tracks = updatedTracks
        _uiState.value = AlbumDetailUiState.Success(currentAlbum, updatedTracks)
    }

    private suspend fun loadLocalTracks(album: Album): List<Track> {
        val albumName = album.name.trim()
        if (albumName.isBlank()) return emptyList()
        val artists = album.artists.map { it.trim() }.filter { it.isNotBlank() }
        if (artists.isEmpty()) return emptyList()
        val localTracks = mutableListOf<Track>()
        for (artistName in artists) {
            localTracks.addAll(
                localMediaRepository.getTracksByAlbum(albumName, artistName).first()
            )
        }
        return localTracks.distinctBy { "${it.provider}:${it.itemId}" }
    }

    private suspend fun resolveLocalAlbum(): Album? {
        val localAlbums = localMediaRepository.getAllAlbums().first()
        val normalizedAlbumId = normalizeLocalAlbumId(albumId)
        val directMatch = localAlbums.firstOrNull { album ->
            album.provider == OFFLINE_PROVIDER &&
                (album.itemId == albumId || album.itemId == normalizedAlbumId)
        }
        if (directMatch != null) return directMatch
        val existing = _album.value
        if (existing != null) {
            val albumName = existing.name.trim()
            val firstArtist = existing.artists.firstOrNull()?.trim()
            if (albumName.isNotBlank() && !firstArtist.isNullOrBlank()) {
                val nameMatch = localMediaRepository
                    .getAlbumByNameAndArtist(albumName, firstArtist)
                    .first()
                if (nameMatch != null) return nameMatch
            }
        }
        if (existing != null) {
            return listOf(existing).mergeWithLocal(localAlbums).firstOrNull()
        }
        return null
    }

    private fun normalizeLocalAlbumId(id: String): String {
        // Navigation args are decoded; re-encode to match stored offline IDs.
        return if (id.isBlank()) id else Uri.encode(Uri.decode(id))
    }

    private fun resolveLocalStartIndex(startIndex: Int, localTracks: List<Track>): Int {
        if (localTracks.isEmpty()) return 0
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val targetId = tracks.getOrNull(safeIndex)?.itemId
        val localIndex = targetId?.let { id ->
            localTracks.indexOfFirst { it.itemId == id }
        } ?: -1
        return if (localIndex >= 0) {
            localIndex
        } else {
            startIndex.coerceIn(0, localTracks.lastIndex)
        }
    }

    private companion object {
        private const val PLAYLIST_LIST_LIMIT = 200
        private const val ARTIST_PREFETCH_LIMIT = 50
        private const val ALBUM_PREFETCH_LIMIT = 50
    }
}
