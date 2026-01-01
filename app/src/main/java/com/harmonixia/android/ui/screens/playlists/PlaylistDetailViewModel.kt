package com.harmonixia.android.ui.screens.playlists

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.data.remote.WebSocketMessage
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import com.harmonixia.android.domain.usecase.DeletePlaylistUseCase
import com.harmonixia.android.domain.usecase.ManagePlaylistTracksUseCase
import com.harmonixia.android.domain.usecase.PlayLocalTracksUseCase
import com.harmonixia.android.domain.usecase.PlayPlaylistUseCase
import com.harmonixia.android.domain.usecase.RenamePlaylistUseCase
import com.harmonixia.android.ui.navigation.Screen
import com.harmonixia.android.util.isLocal
import com.harmonixia.android.util.NetworkConnectivityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

sealed class PlaylistDetailUiEvent {
    data class ShowMessage(val messageResId: Int) : PlaylistDetailUiEvent()
    data object PlaylistCreated : PlaylistDetailUiEvent()
    data object PlaylistDeleted : PlaylistDetailUiEvent()
    data class PlaylistRenamed(val playlist: Playlist) : PlaylistDetailUiEvent()
}

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val localMediaRepository: LocalMediaRepository,
    private val playPlaylistUseCase: PlayPlaylistUseCase,
    private val playLocalTracksUseCase: PlayLocalTracksUseCase,
    private val managePlaylistTracksUseCase: ManagePlaylistTracksUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val playlistId: String =
        savedStateHandle.get<String>(Screen.PlaylistDetail.ARG_PLAYLIST_ID).orEmpty()
    private val provider: String =
        savedStateHandle.get<String>(Screen.PlaylistDetail.ARG_PROVIDER).orEmpty()
    private val isFavoritesPlaylist: Boolean
        get() = playlistId == "favorites" && provider == "harmonixia"

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
    private var remoteTracks: List<Track> = emptyList()
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
        if (isFavoritesPlaylist) {
            loadFavoritesTracks()
            return
        }
        viewModelScope.launch {
            if (playlistId.isBlank() || provider.isBlank()) {
                _uiState.value = PlaylistDetailUiState.Error("Missing playlist details.")
                return@launch
            }
            _uiState.value = PlaylistDetailUiState.Loading
            if (provider == OFFLINE_PROVIDER) {
                _uiState.value = PlaylistDetailUiState.Error(
                    context.getString(R.string.offline_feature_unavailable)
                )
                return@launch
            }
            supervisorScope {
                val playlistsDeferred = async(Dispatchers.IO) {
                    repository.fetchPlaylists(PLAYLIST_LIST_LIMIT, 0)
                }
                val tracksDeferred = async(Dispatchers.IO) {
                    repository.getPlaylistTracks(playlistId, provider)
                }
                val playlistsResult = playlistsDeferred.await()
                val tracksResult = tracksDeferred.await()
                val error = playlistsResult.exceptionOrNull() ?: tracksResult.exceptionOrNull()
                if (error != null && !isOfflineMode.value) {
                    _uiState.value = PlaylistDetailUiState.Error(error.message ?: "Unknown error")
                    return@supervisorScope
                }
                val playlists = playlistsResult.getOrElse { _playlists.value }
                if (playlistsResult.isSuccess) {
                    _playlists.value = playlists
                }
                val selected = playlists.firstOrNull { playlist ->
                    playlist.itemId == playlistId && playlist.provider == provider
                } ?: _playlist.value
                if (selected == null && !isOfflineMode.value) {
                    _uiState.value = PlaylistDetailUiState.Error("Playlist not found.")
                    return@supervisorScope
                }
                selected?.let { _playlist.value = it }
                val loadedTracks = if (tracksResult.isSuccess) {
                    tracksResult.getOrDefault(emptyList())
                } else {
                    tracks.takeIf { it.isNotEmpty() } ?: emptyList()
                }
                if (tracksResult.isSuccess) {
                    remoteTracks = loadedTracks
                }
                val mergedTracks = mergeWithLocalTracks(loadedTracks)
                tracks = mergedTracks
                _uiState.value = if (mergedTracks.isEmpty()) {
                    PlaylistDetailUiState.Empty
                } else {
                    PlaylistDetailUiState.Success(mergedTracks)
                }
            }
        }
    }

    private fun loadFavoritesTracks() {
        viewModelScope.launch {
            _uiState.value = PlaylistDetailUiState.Loading
            val favoritesPlaylist = Playlist(
                itemId = "favorites",
                provider = "harmonixia",
                uri = "harmonixia://favorites",
                name = "Favorites",
                owner = null,
                isEditable = false,
                imageUrl = null
            )
            if (isOfflineMode.value) {
                _playlist.value = favoritesPlaylist
                _uiState.value = PlaylistDetailUiState.Error(
                    "Favorites require an online connection."
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                repository.fetchFavorites(limit = 1000, offset = 0)
            }
            result.onSuccess { loadedTracks ->
                remoteTracks = loadedTracks
                val mergedTracks = mergeWithLocalTracks(loadedTracks)
                tracks = mergedTracks
                _uiState.value = PlaylistDetailUiState.Success(mergedTracks)
            }.onFailure { error ->
                _uiState.value = PlaylistDetailUiState.Error(error.message ?: "Unknown error")
            }
            _playlist.value = favoritesPlaylist
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
                val playlistDeferred = async(Dispatchers.IO) {
                    repository.getPlaylist(playlistId, provider)
                }
                val tracksDeferred = async(Dispatchers.IO) {
                    repository.getPlaylistTracks(playlistId, provider)
                }
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
                if (tracksResult.isSuccess) {
                    remoteTracks = loadedTracks
                }
                val mergedTracks = mergeWithLocalTracks(loadedTracks)
                tracks = mergedTracks
                _uiState.value = if (mergedTracks.isEmpty()) {
                    PlaylistDetailUiState.Empty
                } else {
                    PlaylistDetailUiState.Success(mergedTracks)
                }
            }
        }
    }

    fun playPlaylist(
        startIndex: Int = 0,
        forceStartIndex: Boolean = false,
        shuffleMode: Boolean? = null
    ) {
        if (playlistId.isBlank() || provider.isBlank()) return
        viewModelScope.launch {
            if (isOfflineMode.value) {
                val localTracks = tracks.filter { it.isLocal }
                if (localTracks.isNotEmpty()) {
                    val localIndex = resolveLocalStartIndex(startIndex, localTracks)
                    playLocalTracksUseCase(localTracks, localIndex, shuffleMode)
                }
            } else {
                val cachedTracks = remoteTracks.takeIf { it.isNotEmpty() }
                playPlaylistUseCase(
                    playlistId,
                    provider,
                    startIndex,
                    forceStartIndex,
                    shuffleMode,
                    cachedTracks
                )
            }
        }
    }

    fun playPlaylistSequential() {
        playPlaylist(startIndex = 0, forceStartIndex = false, shuffleMode = false)
    }

    fun shufflePlaylist() {
        if (tracks.isEmpty()) return
        val randomIndex = Random.nextInt(tracks.size)
        playPlaylist(startIndex = randomIndex, forceStartIndex = true, shuffleMode = true)
    }

    fun playTrack(track: Track) {
        if (tracks.isEmpty()) return
        val index = tracks.indexOfFirst { it.itemId == track.itemId }
        if (index >= 0) {
            playPlaylist(index, forceStartIndex = true)
        } else {
            playPlaylist(0, forceStartIndex = true)
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

    fun addTrackToFavorites(track: Track) {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = repository.addToFavorites(track.itemId, track.provider, "track")
            result.onSuccess {
                _events.tryEmit(
                    PlaylistDetailUiEvent.ShowMessage(R.string.track_favorite_add_success)
                )
                loadPlaylistTracks()
            }.onFailure {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_favorite_add_error))
            }
        }
    }

    fun removeTrackFromFavorites(track: Track) {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val result = repository.removeFromFavorites(track.itemId, track.provider, "track")
            result.onSuccess {
                _events.tryEmit(
                    PlaylistDetailUiEvent.ShowMessage(R.string.track_favorite_remove_success)
                )
                loadPlaylistTracks()
            }.onFailure {
                _events.tryEmit(
                    PlaylistDetailUiEvent.ShowMessage(R.string.track_favorite_remove_error)
                )
            }
        }
    }

    fun removeTrackFromPlaylist(track: Track, position: Int) {
        if (isFavoritesPlaylist) {
            removeTrackFromFavorites(track)
            return
        }
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

    private fun resolveTrackPosition(track: Track, position: Int): Int {
        return if (position in tracks.indices && tracks[position].itemId == track.itemId) {
            position
        } else {
            tracks.indexOfFirst { it.itemId == track.itemId }
        }
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

    private fun isPlaylistEvent(event: WebSocketMessage.EventMessage): Boolean {
        val name = event.event.lowercase()
        if ("playlist" in name) return true
        val payload = event.data as? JsonObject ?: return false
        val mediaType = payload["media_type"]?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
        if ("playlist" in mediaType) return true
        val uri = payload["uri"]?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
        return "playlist" in uri
    }

    private suspend fun mergeWithLocalTracks(loadedTracks: List<Track>): List<Track> {
        if (loadedTracks.isEmpty()) return emptyList()
        val localTracks = withContext(Dispatchers.IO) {
            localMediaRepository.getAllTracks().first()
        }
        if (localTracks.isEmpty()) {
            return if (isOfflineMode.value) emptyList() else loadedTracks
        }
        val mergedTracks = withContext(Dispatchers.Default) {
            replaceWithLocalMatches(loadedTracks, localTracks)
        }
        return if (isOfflineMode.value) {
            mergedTracks.filter { it.isLocal }
        } else {
            mergedTracks
        }
    }

    private fun replaceWithLocalMatches(
        loadedTracks: List<Track>,
        localTracks: List<Track>
    ): List<Track> {
        if (loadedTracks.isEmpty() || localTracks.isEmpty()) return loadedTracks
        val localIndicesByKey = HashMap<String, ArrayDeque<Int>>(localTracks.size)
        localTracks.forEachIndexed { index, track ->
            val key = trackMatchKey(track)
            localIndicesByKey.getOrPut(key) { ArrayDeque() }.add(index)
        }
        val merged = ArrayList<Track>(loadedTracks.size)
        for (track in loadedTracks) {
            val queue = localIndicesByKey[trackMatchKey(track)]
            val matchedIndex = if (queue != null && queue.isNotEmpty()) {
                queue.removeFirst()
            } else {
                -1
            }
            if (matchedIndex >= 0) {
                merged.add(localTracks[matchedIndex])
            } else {
                merged.add(track)
            }
        }
        return merged
    }

    private fun trackMatchKey(track: Track): String {
        return "${normalizeMatchKey(track.title)}::${normalizeMatchKey(track.artist)}::${normalizeMatchKey(track.album)}"
    }

    private fun normalizeMatchKey(value: String): String {
        return value.trim().lowercase()
    }

    companion object {
        private const val PLAYLIST_LIST_LIMIT = 200
    }
}
