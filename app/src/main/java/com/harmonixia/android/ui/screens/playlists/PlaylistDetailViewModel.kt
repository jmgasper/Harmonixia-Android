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
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.isLocal
import com.harmonixia.android.util.NetworkConnectivityManager
import com.harmonixia.android.util.PerformanceMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.yield
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
    private val performanceMonitor: PerformanceMonitor,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    val imageQualityManager: ImageQualityManager
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

    private val _isSavingReorder = MutableStateFlow(false)
    val isSavingReorder: StateFlow<Boolean> = _isSavingReorder.asStateFlow()

    private val _events = MutableSharedFlow<PlaylistDetailUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var tracks: List<Track> = emptyList()
    private var remoteTracks: List<Track> = emptyList()
    private var currentOffset = 0
    private var hasMoreTracks = false
    private var isLoadingMoreTracks = false
    private var loadMoreJob: Job? = null
    private var loadAllJob: Job? = null
    private var isFullTrackListLoaded = false
    private var localTracksCache: List<Track>? = null
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
            loadPlaylistTracksInternal(showLoading = true, isRefresh = false)
        }
    }

    private fun loadFavoritesTracks() {
        viewModelScope.launch {
            hasMoreTracks = false
            isLoadingMoreTracks = false
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
            if (isFavoritesPlaylist) {
                loadFavoritesTracks()
                return@launch
            }
            loadPlaylistTracksInternal(showLoading = showLoading, isRefresh = true)
        }
    }

    private suspend fun loadPlaylistTracksInternal(
        showLoading: Boolean,
        isRefresh: Boolean
    ) {
        if (playlistId.isBlank() || provider.isBlank()) {
            _uiState.value = PlaylistDetailUiState.Error("Missing playlist details.")
            return
        }
        if (provider == OFFLINE_PROVIDER) {
            _uiState.value = PlaylistDetailUiState.Error(
                context.getString(R.string.offline_feature_unavailable)
            )
            return
        }
        val detailKey = detailKey()
        if (!isRefresh) {
            performanceMonitor.markDetailLoadStart(PerformanceMonitor.DetailType.PLAYLIST, detailKey)
        }
        val cachedPlaylist = repository.getCachedPlaylist(playlistId, provider)
        cachedPlaylist?.let { _playlist.value = it }
        val cachedTracks = repository.getCachedPlaylistTracks(playlistId, provider)
        val existingTracks = tracks.takeIf { it.isNotEmpty() }
        val cachedForDisplay = cachedTracks ?: existingTracks

        if (isOfflineMode.value) {
            if (cachedForDisplay == null) {
                _uiState.value = PlaylistDetailUiState.Error(
                    context.getString(R.string.offline_feature_unavailable)
                )
                return
            }
            val mergedCached = mergeWithLocalTracks(cachedForDisplay)
            tracks = mergedCached
            _uiState.value = if (mergedCached.isEmpty()) {
                PlaylistDetailUiState.Empty
            } else {
                PlaylistDetailUiState.Cached(mergedCached, isRefreshing = false)
            }
            return
        }

        if (cachedForDisplay != null) {
            val mergedCached = mergeWithLocalTracks(cachedForDisplay)
            tracks = mergedCached
            _uiState.value = PlaylistDetailUiState.Cached(mergedCached, isRefreshing = true)
            performanceMonitor.markDetailCacheShown(PerformanceMonitor.DetailType.PLAYLIST, detailKey)
        } else if (showLoading) {
            _uiState.value = PlaylistDetailUiState.Loading
        }

        loadMoreJob?.cancel()
        loadMoreJob = null
        loadAllJob?.cancel()
        loadAllJob = null
        currentOffset = 0
        hasMoreTracks = false
        isLoadingMoreTracks = false
        isFullTrackListLoaded = false

        supervisorScope {
            val playlistDeferred = async(Dispatchers.IO) {
                repository.getPlaylist(playlistId, provider)
            }
            val tracksDeferred = async(Dispatchers.IO) {
                repository.getPlaylistTracksChunked(
                    playlistId,
                    provider,
                    0,
                    INITIAL_TRACK_CHUNK_SIZE
                )
            }
            val playlistResult = playlistDeferred.await()
            playlistResult.getOrNull()?.let { loadedPlaylist ->
                _playlist.value = loadedPlaylist
                if (cachedForDisplay == null && _uiState.value is PlaylistDetailUiState.Loading) {
                    _uiState.value = PlaylistDetailUiState.Metadata
                }
            }
            val tracksResult = tracksDeferred.await()
            val error = playlistResult.exceptionOrNull() ?: tracksResult.exceptionOrNull()
            if (error != null) {
                if (cachedForDisplay == null) {
                    _uiState.value = PlaylistDetailUiState.Error(error.message ?: "Unknown error")
                }
                return@supervisorScope
            }
            val loadedTracks = tracksResult.getOrDefault(emptyList())
            val cachedFullTracks = repository.getCachedPlaylistTracks(playlistId, provider)
            if (cachedFullTracks != null) {
                remoteTracks = cachedFullTracks
            } else if (tracksResult.isSuccess) {
                remoteTracks = loadedTracks
            }
            val mergedTracks = mergeWithLocalTracks(loadedTracks)
            tracks = mergedTracks
            val totalCount = cachedFullTracks?.size ?: loadedTracks.size
            currentOffset = mergedTracks.size
            hasMoreTracks = totalCount > mergedTracks.size
            isFullTrackListLoaded = !hasMoreTracks
            isLoadingMoreTracks = false
            _uiState.value = if (mergedTracks.isEmpty()) {
                PlaylistDetailUiState.Empty
            } else {
                PlaylistDetailUiState.Success(
                    mergedTracks,
                    hasMore = hasMoreTracks,
                    isLoadingMore = false
                )
            }
            performanceMonitor.markDetailFreshLoaded(
                PerformanceMonitor.DetailType.PLAYLIST,
                detailKey
            )
            if (!hasMoreTracks && mergedTracks.isNotEmpty()) {
                performanceMonitor.markTrackListLoaded(
                    PerformanceMonitor.DetailType.PLAYLIST,
                    detailKey,
                    mergedTracks.size
                )
            }
            if (hasMoreTracks && mergedTracks.isNotEmpty()) {
                startAutoLoadAllTracks()
            }
        }
    }

    fun loadMoreTracks() {
        if (isFavoritesPlaylist || isOfflineMode.value) return
        if (isFullTrackListLoaded || !hasMoreTracks || isLoadingMoreTracks) return
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            loadMoreTracksInternal()
        }
    }

    private suspend fun loadMoreTracksInternal() {
        if (playlistId.isBlank() || provider.isBlank()) return
        if (isLoadingMoreTracks || !hasMoreTracks || isFullTrackListLoaded) return
        isLoadingMoreTracks = true
        updateLoadingMoreState(true)
        val detailKey = detailKey()
        val result = repository.getPlaylistTracksChunked(
            playlistId,
            provider,
            currentOffset,
            SUBSEQUENT_CHUNK_SIZE
        )
        result.onSuccess { chunk ->
            val cachedFullTracks = repository.getCachedPlaylistTracks(playlistId, provider)
            val fullTracks = cachedFullTracks ?: (remoteTracks + chunk)
            remoteTracks = fullTracks
            val mergedTracks = mergeWithLocalTracks(fullTracks)
            tracks = mergedTracks
            currentOffset = fullTracks.size
            if (cachedFullTracks != null) {
                hasMoreTracks = false
                isFullTrackListLoaded = true
            } else if (!isFullTrackListLoaded) {
                if (chunk.isEmpty()) {
                    hasMoreTracks = false
                    isFullTrackListLoaded = true
                } else if (chunk.size < SUBSEQUENT_CHUNK_SIZE) {
                    hasMoreTracks = false
                    isFullTrackListLoaded = true
                } else {
                    hasMoreTracks = true
                }
            } else {
                hasMoreTracks = false
            }
            _uiState.value = if (mergedTracks.isEmpty()) {
                PlaylistDetailUiState.Empty
            } else {
                PlaylistDetailUiState.Success(
                    mergedTracks,
                    hasMore = hasMoreTracks,
                    isLoadingMore = false
                )
            }
            if (!hasMoreTracks && mergedTracks.isNotEmpty()) {
                performanceMonitor.markTrackListLoaded(
                    PerformanceMonitor.DetailType.PLAYLIST,
                    detailKey,
                    mergedTracks.size
                )
            }
        }.onFailure {
            hasMoreTracks = false
            _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.playlists_error))
        }
        isLoadingMoreTracks = false
        updateLoadingMoreState(false)
    }

    private fun startAutoLoadAllTracks() {
        if (isFavoritesPlaylist || isOfflineMode.value) return
        if (isFullTrackListLoaded || !hasMoreTracks) return
        if (loadAllJob?.isActive == true) return
        loadMoreJob?.cancel()
        loadMoreJob = null
        loadAllJob = viewModelScope.launch {
            loadAllTracksInternal()
        }
    }

    private suspend fun loadAllTracksInternal() {
        if (playlistId.isBlank() || provider.isBlank()) return
        if (isFullTrackListLoaded || isFavoritesPlaylist || isOfflineMode.value) return
        if (isLoadingMoreTracks) return
        isLoadingMoreTracks = true
        updateLoadingMoreState(true)
        val detailKey = detailKey()
        val result = repository.getPlaylistTracks(playlistId, provider)
        result.onSuccess { fullTracks ->
            remoteTracks = fullTracks
            val mergedTracks = mergeWithLocalTracks(fullTracks)
            tracks = mergedTracks
            currentOffset = fullTracks.size
            hasMoreTracks = false
            isFullTrackListLoaded = true
            isLoadingMoreTracks = false
            _uiState.value = if (mergedTracks.isEmpty()) {
                PlaylistDetailUiState.Empty
            } else {
                PlaylistDetailUiState.Success(
                    mergedTracks,
                    hasMore = false,
                    isLoadingMore = false
                )
            }
            if (mergedTracks.isNotEmpty()) {
                performanceMonitor.markTrackListLoaded(
                    PerformanceMonitor.DetailType.PLAYLIST,
                    detailKey,
                    mergedTracks.size
                )
            }
        }.onFailure {
            isLoadingMoreTracks = false
            updateLoadingMoreState(false)
        }
    }

    private fun updateLoadingMoreState(isLoading: Boolean) {
        val currentTracks = tracks
        if (currentTracks.isEmpty()) return
        _uiState.value = PlaylistDetailUiState.Success(
            currentTracks,
            hasMore = hasMoreTracks,
            isLoadingMore = isLoading
        )
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
                val playlistUri = _playlist.value?.uri?.takeIf { it.isNotBlank() }
                    ?: repository.getCachedPlaylist(playlistId, provider)?.uri
                playPlaylistUseCase(
                    playlistId = playlistId,
                    provider = provider,
                    startIndex = startIndex,
                    forceStartIndex = forceStartIndex,
                    shuffleMode = shuffleMode,
                    tracksOverride = cachedTracks,
                    playlistUri = playlistUri
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
                    _uiState.value = PlaylistDetailUiState.Success(
                        updated,
                        hasMore = hasMoreTracks,
                        isLoadingMore = false
                    )
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
            val targetTrack = resolveFavoriteTarget(track)
            if (targetTrack == null) {
                _events.tryEmit(
                    PlaylistDetailUiEvent.ShowMessage(R.string.track_favorite_offline_unavailable)
                )
                return@launch
            }
            val result = repository.addToFavorites(targetTrack)
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
            val targetTrack = resolveFavoriteTarget(track)
            if (targetTrack == null) {
                _events.tryEmit(
                    PlaylistDetailUiEvent.ShowMessage(R.string.track_favorite_offline_unavailable)
                )
                return@launch
            }
            val result = repository.removeFromFavorites(targetTrack)
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
                    PlaylistDetailUiState.Success(
                        updated,
                        hasMore = hasMoreTracks,
                        isLoadingMore = false
                    )
                }
                refreshPlaylist()
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_remove_success))
            }.onFailure {
                _events.tryEmit(PlaylistDetailUiEvent.ShowMessage(R.string.track_remove_error))
            }
        }
    }

    fun saveReorderedTracks(orderedTracks: List<Track>) {
        val playlist = _playlist.value ?: return
        if (isFavoritesPlaylist || !playlist.isEditable) return
        if (hasMoreTracks || isLoadingMoreTracks) {
            viewModelScope.launch {
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_reorder_load_all))
            }
            return
        }
        if (orderedTracks.isEmpty()) return
        val currentTracks = tracks
        if (currentTracks.isEmpty()) return
        if (orderedTracks.map(::reorderKey) == currentTracks.map(::reorderKey)) {
            return
        }
        viewModelScope.launch {
            if (_isSavingReorder.value) return@launch
            _isSavingReorder.value = true
            if (isOfflineMode.value) {
                _isSavingReorder.value = false
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.offline_feature_unavailable))
                return@launch
            }
            val positionsByKey = mutableMapOf<String, ArrayDeque<Int>>()
            currentTracks.forEachIndexed { index, track ->
                val key = reorderKey(track)
                positionsByKey.getOrPut(key) { ArrayDeque() }.add(index)
            }
            val reorderedRemote = orderedTracks.mapNotNull { track ->
                val key = reorderKey(track)
                val queue = positionsByKey[key]
                val index = queue?.removeFirstOrNull()
                index?.let { remoteTracks.getOrNull(it) }
            }
            if (reorderedRemote.size != orderedTracks.size) {
                _isSavingReorder.value = false
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_reorder_error))
                return@launch
            }
            val newUris = reorderedRemote.mapNotNull { it.uri.takeIf { uri -> uri.isNotBlank() } }
            if (newUris.size != reorderedRemote.size) {
                _isSavingReorder.value = false
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_reorder_error))
                return@launch
            }
            val originalUris = remoteTracks.mapNotNull { it.uri.takeIf { uri -> uri.isNotBlank() } }
            val positions = currentTracks.indices.toList().sortedDescending()
            val removeResult = managePlaylistTracksUseCase.removeTracksFromPlaylist(
                playlistId = playlist.itemId,
                positions = positions
            )
            val addResult = if (removeResult.isSuccess) {
                managePlaylistTracksUseCase.addTracksToPlaylist(
                    playlistId = playlist.itemId,
                    trackUris = newUris,
                    isEditable = playlist.isEditable
                )
            } else {
                removeResult
            }
            if (addResult.isSuccess) {
                tracks = orderedTracks
                remoteTracks = reorderedRemote
                _uiState.value = PlaylistDetailUiState.Success(
                    orderedTracks,
                    hasMore = hasMoreTracks,
                    isLoadingMore = false
                )
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_reorder_success))
            } else {
                if (removeResult.isSuccess && originalUris.isNotEmpty()) {
                    managePlaylistTracksUseCase.addTracksToPlaylist(
                        playlistId = playlist.itemId,
                        trackUris = originalUris,
                        isEditable = playlist.isEditable
                    )
                }
                _events.emit(PlaylistDetailUiEvent.ShowMessage(R.string.playlist_reorder_error))
            }
            _isSavingReorder.value = false
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
        val localTracks = localTracksCache ?: withContext(Dispatchers.IO) {
            localMediaRepository.getAllTracks().first()
        }.also { localTracksCache = it }
        if (localTracks.isEmpty()) {
            return if (isOfflineMode.value) emptyList() else loadedTracks
        }
        val mergedTracks = replaceWithLocalMatches(loadedTracks, localTracks)
        return if (isOfflineMode.value) {
            mergedTracks.filter { it.isLocal }
        } else {
            mergedTracks
        }
    }

    private suspend fun replaceWithLocalMatches(
        loadedTracks: List<Track>,
        localTracks: List<Track>
    ): List<Track> = withContext(Dispatchers.Default) {
        if (loadedTracks.isEmpty() || localTracks.isEmpty()) return@withContext loadedTracks
        val localIndicesByKey = HashMap<String, ArrayDeque<Int>>(localTracks.size)
        localTracks.forEachIndexed { index, track ->
            if (index % MERGE_BATCH_SIZE == 0) {
                yield()
            }
            val key = trackMatchKey(track)
            localIndicesByKey.getOrPut(key) { ArrayDeque() }.add(index)
        }
        val merged = ArrayList<Track>(loadedTracks.size)
        for ((index, track) in loadedTracks.withIndex()) {
            if (index % MERGE_BATCH_SIZE == 0) {
                yield()
            }
            val queue = localIndicesByKey[trackMatchKey(track)]
            val matchedIndex = if (queue != null && queue.isNotEmpty()) {
                queue.removeFirst()
            } else {
                -1
            }
            if (matchedIndex >= 0) {
                val local = localTracks[matchedIndex]
                merged.add(local.copy(isFavorite = track.isFavorite))
            } else {
                merged.add(track)
            }
        }
        merged
    }

    private fun trackMatchKey(track: Track): String {
        return "${normalizeMatchKey(track.title)}::${normalizeMatchKey(track.artist)}::${normalizeMatchKey(track.album)}"
    }

    private suspend fun resolveFavoriteTarget(track: Track): Track? {
        if (track.provider != OFFLINE_PROVIDER) return track
        val remoteMatch = pickBestFavoriteMatch(track, remoteTracks)
        if (remoteMatch != null) return remoteMatch
        val query = buildFavoriteSearchQuery(track)
        if (query.isBlank()) return null
        val searchResults = repository.searchLibrary(query, FAVORITE_SEARCH_LIMIT).getOrNull()
            ?: return null
        return pickBestFavoriteMatch(track, searchResults.tracks)
    }

    private fun pickBestFavoriteMatch(track: Track, candidates: List<Track>): Track? {
        if (candidates.isEmpty()) return null
        val titleKey = normalizeMatchKey(track.title)
        if (titleKey.isBlank()) return null
        val artistKey = normalizeMatchKey(track.artist)
        val albumKey = normalizeMatchKey(track.album)
        val trackNumber = track.trackNumber.takeIf { it > 0 }
        return candidates.asSequence()
            .filter { it.provider != OFFLINE_PROVIDER }
            .mapNotNull { candidate ->
                if (normalizeMatchKey(candidate.title) != titleKey) return@mapNotNull null
                if (artistKey.isNotBlank() &&
                    normalizeMatchKey(candidate.artist) != artistKey
                ) return@mapNotNull null
                if (albumKey.isNotBlank() &&
                    normalizeMatchKey(candidate.album) != albumKey
                ) return@mapNotNull null
                var score = 1
                if (artistKey.isNotBlank()) score += 1
                if (albumKey.isNotBlank()) score += 1
                if (trackNumber != null && candidate.trackNumber == trackNumber) {
                    score += 2
                }
                candidate to score
            }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun buildFavoriteSearchQuery(track: Track): String {
        return listOf(track.title, track.artist, track.album)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun normalizeMatchKey(value: String): String {
        return value.trim().lowercase()
    }

    private fun reorderKey(track: Track): String {
        return "${track.itemId}::${track.provider}"
    }

    private fun detailKey(): String {
        return "${playlistId.trim()}:${provider.trim()}"
    }

    companion object {
        private const val PLAYLIST_LIST_LIMIT = 200
        private const val INITIAL_TRACK_CHUNK_SIZE = 50
        private const val SUBSEQUENT_CHUNK_SIZE = 150
        private const val MERGE_BATCH_SIZE = 100
        private const val FAVORITE_SEARCH_LIMIT = 50
    }
}
