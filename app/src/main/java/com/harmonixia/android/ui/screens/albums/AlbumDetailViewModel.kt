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
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.isLocal
import com.harmonixia.android.util.PerformanceMonitor
import com.harmonixia.android.util.mergeWithLocal
import com.harmonixia.android.util.NetworkConnectivityManager
import com.harmonixia.android.util.PrefetchScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlin.random.Random

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
    private val performanceMonitor: PerformanceMonitor,
    savedStateHandle: SavedStateHandle,
    val imageQualityManager: ImageQualityManager
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
    private var remoteTracks: List<Track> = emptyList()
    private var currentOffset = 0
    private var hasMoreTracks = false
    private var isLoadingMoreTracks = false
    private var loadMoreJob: Job? = null
    private var localAlbumTracks: List<Track> = emptyList()
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
            loadAlbumTracksInternal(showLoading = true, isRefresh = false)
        }
    }

    fun refreshAlbum(showLoading: Boolean = false) {
        viewModelScope.launch {
            loadAlbumTracksInternal(showLoading = showLoading, isRefresh = true)
        }
    }

    private suspend fun loadAlbumTracksInternal(
        showLoading: Boolean,
        isRefresh: Boolean
    ) {
        if (albumId.isBlank() || provider.isBlank()) {
            _uiState.value = AlbumDetailUiState.Error("Missing album details.")
            return
        }
        val detailKey = detailKey()
        if (!isRefresh) {
            performanceMonitor.markDetailLoadStart(PerformanceMonitor.DetailType.ALBUM, detailKey)
        }
        if (provider == OFFLINE_PROVIDER) {
            val localAlbum = resolveLocalAlbum()
            if (localAlbum == null) {
                _uiState.value = AlbumDetailUiState.Error("Album not found.")
                return
            }
            val localTracks = loadLocalTracks(localAlbum)
            localAlbumTracks = localTracks
            currentOffset = localTracks.size
            hasMoreTracks = false
            isLoadingMoreTracks = false
            _album.value = localAlbum
            tracks = localTracks
            remoteTracks = emptyList()
            _uiState.value = if (localTracks.isEmpty()) {
                AlbumDetailUiState.Empty
            } else {
                AlbumDetailUiState.Success(localAlbum, localTracks)
            }
            return
        }

        val cachedAlbum = repository.getCachedAlbum(albumId, provider)
        val cachedTracks = repository.getCachedAlbumTracks(albumId, provider)
        cachedAlbum?.let { _album.value = it }

        if (isOfflineMode.value) {
            if (cachedAlbum == null || cachedTracks == null) {
                _uiState.value = AlbumDetailUiState.Error(
                    "Offline cache unavailable for this album."
                )
                return
            }
            remoteTracks = cachedTracks
            localAlbumTracks = loadLocalTracks(cachedAlbum)
            val mergedCached = mergeWithLocalTracks(
                cachedTracks,
                localAlbumTracks,
                appendUnmatchedLocal = true
            )
            tracks = mergedCached
            currentOffset = mergedCached.size
            hasMoreTracks = false
            isLoadingMoreTracks = false
            _uiState.value = if (mergedCached.isEmpty()) {
                AlbumDetailUiState.Empty
            } else {
                AlbumDetailUiState.Cached(cachedAlbum, mergedCached, isRefreshing = false)
            }
            return
        }

        if (cachedAlbum != null && cachedTracks != null) {
            remoteTracks = cachedTracks
            localAlbumTracks = loadLocalTracks(cachedAlbum)
            val mergedCached = mergeWithLocalTracks(
                cachedTracks,
                localAlbumTracks,
                appendUnmatchedLocal = true
            )
            tracks = mergedCached
            _uiState.value = AlbumDetailUiState.Cached(
                cachedAlbum,
                mergedCached,
                isRefreshing = true
            )
            performanceMonitor.markDetailCacheShown(
                PerformanceMonitor.DetailType.ALBUM,
                detailKey
            )
        } else if (showLoading) {
            _uiState.value = AlbumDetailUiState.Loading
        }

        loadMoreJob?.cancel()
        loadMoreJob = null
        currentOffset = 0
        hasMoreTracks = false
        isLoadingMoreTracks = false

        supervisorScope {
            val albumDeferred = async(Dispatchers.IO) {
                repository.getAlbum(albumId, provider)
            }
            val tracksDeferred = async(Dispatchers.IO) {
                repository.getAlbumTracksChunked(
                    albumId,
                    provider,
                    0,
                    INITIAL_TRACK_CHUNK_SIZE
                )
            }
            val albumResult = albumDeferred.await()
            albumResult.getOrNull()?.let { loadedAlbum ->
                _album.value = loadedAlbum
                if (_uiState.value is AlbumDetailUiState.Loading) {
                    _uiState.value = AlbumDetailUiState.Metadata
                }
            }
            val tracksResult = tracksDeferred.await()
            val error = albumResult.exceptionOrNull() ?: tracksResult.exceptionOrNull()
            if (error != null) {
                if (cachedAlbum == null) {
                    _uiState.value = AlbumDetailUiState.Error(error.message ?: "Unknown error")
                }
                return@supervisorScope
            }
            val loadedAlbum = albumResult.getOrThrow()
            val loadedTracks = tracksResult.getOrDefault(emptyList())
            localAlbumTracks = loadLocalTracks(loadedAlbum)
            val cachedFullTracks = repository.getCachedAlbumTracks(albumId, provider)
            remoteTracks = cachedFullTracks ?: loadedTracks
            val mergedTracks = mergeWithLocalTracks(
                loadedTracks,
                localAlbumTracks,
                appendUnmatchedLocal = false
            )
            tracks = mergedTracks
            val totalCount = cachedFullTracks?.size ?: loadedTracks.size
            currentOffset = mergedTracks.size
            hasMoreTracks = totalCount > mergedTracks.size
            isLoadingMoreTracks = false
            prefetchArtistData(loadedAlbum)
            _uiState.value = if (mergedTracks.isEmpty()) {
                AlbumDetailUiState.Empty
            } else {
                AlbumDetailUiState.Success(
                    loadedAlbum,
                    mergedTracks,
                    hasMore = hasMoreTracks,
                    isLoadingMore = false
                )
            }
            performanceMonitor.markDetailFreshLoaded(
                PerformanceMonitor.DetailType.ALBUM,
                detailKey
            )
            if (!hasMoreTracks && mergedTracks.isNotEmpty()) {
                val finalMerged = mergeWithLocalTracks(
                    loadedTracks,
                    localAlbumTracks,
                    appendUnmatchedLocal = true
                )
                if (finalMerged != mergedTracks) {
                    tracks = finalMerged
                    _uiState.value = AlbumDetailUiState.Success(
                        loadedAlbum,
                        finalMerged,
                        hasMore = false,
                        isLoadingMore = false
                    )
                }
                performanceMonitor.markTrackListLoaded(
                    PerformanceMonitor.DetailType.ALBUM,
                    detailKey,
                    finalMerged.size
                )
            }
        }
    }

    fun loadMoreTracks() {
        if (isOfflineMode.value || provider == OFFLINE_PROVIDER) return
        if (albumId.isBlank() || provider.isBlank()) return
        if (!hasMoreTracks || isLoadingMoreTracks) return
        val currentAlbum = _album.value ?: return
        isLoadingMoreTracks = true
        updateLoadingMoreState(true, currentAlbum)
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            try {
                loadMoreTracksInternal(currentAlbum)
            } finally {
                isLoadingMoreTracks = false
                updateLoadingMoreState(false, currentAlbum)
            }
        }
    }

    private suspend fun loadMoreTracksInternal(currentAlbum: Album) {
        if (!hasMoreTracks) return
        val detailKey = detailKey()
        val result = repository.getAlbumTracksChunked(
            albumId,
            provider,
            currentOffset,
            SUBSEQUENT_CHUNK_SIZE
        )
        result.onSuccess { chunk ->
            if (chunk.isEmpty()) {
                hasMoreTracks = false
            } else {
                currentOffset += chunk.size
                hasMoreTracks = chunk.size >= SUBSEQUENT_CHUNK_SIZE
            }
            val cachedFullTracks = repository.getCachedAlbumTracks(albumId, provider)
            val fullTracks = cachedFullTracks ?: (remoteTracks + chunk)
            remoteTracks = fullTracks
            val mergedTracks = mergeWithLocalTracks(
                fullTracks,
                localAlbumTracks,
                appendUnmatchedLocal = !hasMoreTracks
            )
            tracks = mergedTracks
            _uiState.value = if (mergedTracks.isEmpty()) {
                AlbumDetailUiState.Empty
            } else {
                AlbumDetailUiState.Success(
                    currentAlbum,
                    mergedTracks,
                    hasMore = hasMoreTracks,
                    isLoadingMore = false
                )
            }
            if (!hasMoreTracks && mergedTracks.isNotEmpty()) {
                performanceMonitor.markTrackListLoaded(
                    PerformanceMonitor.DetailType.ALBUM,
                    detailKey,
                    mergedTracks.size
                )
            }
        }.onFailure {
            hasMoreTracks = false
            _events.tryEmit(AlbumDetailUiEvent.ShowMessage(R.string.albums_error))
        }
    }

    private fun updateLoadingMoreState(isLoading: Boolean, album: Album) {
        val currentTracks = tracks
        if (currentTracks.isEmpty()) return
        _uiState.value = AlbumDetailUiState.Success(
            album,
            currentTracks,
            hasMore = hasMoreTracks,
            isLoadingMore = isLoading
        )
    }

    private suspend fun mergeWithLocalTracks(
        loadedTracks: List<Track>,
        localTracks: List<Track>,
        appendUnmatchedLocal: Boolean
    ): List<Track> {
        if (loadedTracks.isEmpty()) {
            return if (appendUnmatchedLocal) localTracks else emptyList()
        }
        if (localTracks.isEmpty()) return loadedTracks
        return replaceWithLocalMatches(
            loadedTracks,
            localTracks,
            appendUnmatchedLocal
        )
    }

    private suspend fun replaceWithLocalMatches(
        loadedTracks: List<Track>,
        localTracks: List<Track>,
        appendUnmatchedLocal: Boolean
    ): List<Track> = withContext(Dispatchers.Default) {
        if (loadedTracks.isEmpty() || localTracks.isEmpty()) return@withContext loadedTracks
        val localIndicesByKey = HashMap<String, ArrayDeque<Int>>(localTracks.size)
        val usedLocal = BooleanArray(localTracks.size)
        localTracks.forEachIndexed { index, track ->
            if (index % MERGE_BATCH_SIZE == 0) {
                yield()
            }
            val key = trackMatchKey(track)
            localIndicesByKey.getOrPut(key) { ArrayDeque() }.add(index)
        }
        val merged = ArrayList<Track>(loadedTracks.size + if (appendUnmatchedLocal) localTracks.size else 0)
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
                usedLocal[matchedIndex] = true
                val local = localTracks[matchedIndex]
                merged.add(local.copy(isFavorite = track.isFavorite))
            } else {
                merged.add(track)
            }
        }
        if (appendUnmatchedLocal) {
            for (index in localTracks.indices) {
                if (index % MERGE_BATCH_SIZE == 0) {
                    yield()
                }
                if (!usedLocal[index]) {
                    merged.add(localTracks[index])
                }
            }
        }
        merged
    }

    fun playAlbum(
        startIndex: Int = 0,
        forceStartIndex: Boolean = false,
        shuffleMode: Boolean? = null
    ) {
        if (albumId.isBlank() || provider.isBlank()) return
        viewModelScope.launch {
            if (isOfflineMode.value || provider == OFFLINE_PROVIDER) {
                val localTracks = tracks.filter { it.isLocal }
                if (localTracks.isNotEmpty()) {
                    val localIndex = resolveLocalStartIndex(startIndex, localTracks)
                    playLocalTracksUseCase(localTracks, localIndex, shuffleMode)
                }
            } else {
                val resolvedAlbumUri = _album.value?.uri?.takeIf { it.isNotBlank() }
                    ?: repository.getCachedAlbum(albumId, provider)?.uri
                val cachedTracks = remoteTracks.takeIf { it.isNotEmpty() }
                playAlbumUseCase(
                    albumId = albumId,
                    provider = provider,
                    startIndex = startIndex,
                    forceStartIndex = forceStartIndex,
                    shuffleMode = shuffleMode,
                    tracksOverride = cachedTracks,
                    albumUri = resolvedAlbumUri
                )
            }
        }
    }

    fun playAlbumSequential() {
        playAlbum(startIndex = 0, forceStartIndex = false, shuffleMode = false)
    }

    fun shuffleAlbum() {
        if (tracks.isEmpty()) return
        val randomIndex = Random.nextInt(tracks.size)
        playAlbum(startIndex = randomIndex, forceStartIndex = true, shuffleMode = true)
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
            val targetTrack = resolveFavoriteTarget(track)
            if (targetTrack == null) {
                _events.tryEmit(
                    AlbumDetailUiEvent.ShowMessage(R.string.track_favorite_offline_unavailable)
                )
                return@launch
            }
            val result = repository.addToFavorites(targetTrack)
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
            val targetTrack = resolveFavoriteTarget(track)
            if (targetTrack == null) {
                _events.tryEmit(
                    AlbumDetailUiEvent.ShowMessage(R.string.track_favorite_offline_unavailable)
                )
                return@launch
            }
            val result = repository.removeFromFavorites(targetTrack)
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
        val currentState = uiState.value
        val currentAlbum = when (currentState) {
            is AlbumDetailUiState.Success -> currentState.album
            is AlbumDetailUiState.Cached -> currentState.album
            else -> album.value
        } ?: return
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
        _uiState.value = when (currentState) {
            is AlbumDetailUiState.Cached -> AlbumDetailUiState.Cached(
                currentAlbum,
                updatedTracks,
                isRefreshing = currentState.isRefreshing
            )
            is AlbumDetailUiState.Success -> AlbumDetailUiState.Success(
                currentAlbum,
                updatedTracks,
                hasMore = currentState.hasMore,
                isLoadingMore = currentState.isLoadingMore
            )
            else -> AlbumDetailUiState.Success(currentAlbum, updatedTracks)
        }
    }

    private suspend fun loadLocalTracks(album: Album): List<Track> {
        if (localAlbumTracks.isNotEmpty()) return localAlbumTracks
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
        val resolvedTracks = localTracks.distinctBy { "${it.provider}:${it.itemId}" }
        localAlbumTracks = resolvedTracks
        return resolvedTracks
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

    private fun detailKey(): String {
        return "${albumId.trim()}:${provider.trim()}"
    }

    private companion object {
        private const val PLAYLIST_LIST_LIMIT = 200
        private const val ARTIST_PREFETCH_LIMIT = 50
        private const val ALBUM_PREFETCH_LIMIT = 50
        private const val INITIAL_TRACK_CHUNK_SIZE = 50
        private const val SUBSEQUENT_CHUNK_SIZE = 150
        private const val MERGE_BATCH_SIZE = 100
        private const val FAVORITE_SEARCH_LIMIT = 50
    }
}
