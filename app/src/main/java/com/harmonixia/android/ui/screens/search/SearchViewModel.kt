package com.harmonixia.android.ui.screens.search

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.ImageRequest
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.OfflineLibraryRepository
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.domain.usecase.SearchLibraryUseCase
import com.harmonixia.android.util.mergeWithLocal
import com.harmonixia.android.util.NetworkConnectivityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchLibraryUseCase: SearchLibraryUseCase,
    private val offlineLibraryRepository: OfflineLibraryRepository,
    private val localMediaRepository: LocalMediaRepository,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val networkConnectivityManager: NetworkConnectivityManager
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

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SEARCH_QUERY).orEmpty()
    )
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val sectionLimits = MutableStateFlow(SectionLimits())
    private val cache = LinkedHashMap<String, CachedResult>()
    private var lastResults: SearchResults? = null
    private var lastQuery: String? = null

    init {
        viewModelScope.launch {
            isOfflineMode.collect { offline ->
                if (offline) {
                    cache.clear()
                    lastResults = null
                    lastQuery = null
                }
            }
        }
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { query ->
                    val trimmed = query.trim()
                    if (trimmed.isBlank()) {
                        _uiState.value = SearchUiState.Idle
                        return@collectLatest
                    }
                    sectionLimits.value = SectionLimits()
                    performSearch(trimmed)
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun retrySearch() {
        val trimmed = _searchQuery.value.trim()
        if (trimmed.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }
        viewModelScope.launch {
            performSearch(trimmed)
        }
    }

    fun showMoreAlbums() {
        updateLimits { current ->
            current.copy(albumLimit = current.albumLimit + SHOW_MORE_INCREMENT)
        }
    }

    fun showMoreArtists() {
        updateLimits { current ->
            current.copy(artistLimit = current.artistLimit + SHOW_MORE_INCREMENT)
        }
    }

    fun showMorePlaylists() {
        updateLimits { current ->
            current.copy(playlistLimit = current.playlistLimit + SHOW_MORE_INCREMENT)
        }
    }

    fun showMoreTracks() {
        updateLimits { current ->
            current.copy(trackLimit = current.trackLimit + SHOW_MORE_INCREMENT)
        }
    }

    private fun updateLimits(update: (SectionLimits) -> SectionLimits) {
        val updated = update(sectionLimits.value)
        sectionLimits.value = updated
        val results = lastResults ?: return
        val query = lastQuery ?: return
        val isEmpty = results.isEmpty()
        _uiState.value = SearchUiState.Success(
            query = query,
            results = results,
            isEmpty = isEmpty,
            albumLimit = updated.albumLimit,
            artistLimit = updated.artistLimit,
            playlistLimit = updated.playlistLimit,
            trackLimit = updated.trackLimit
        )
    }

    private suspend fun performSearch(query: String) {
        val offline = networkConnectivityManager.isOfflineMode() ||
            connectionState.value !is ConnectionState.Connected
        _uiState.value = SearchUiState.Loading
        val results = if (offline) {
            offlineLibraryRepository.searchDownloadedContent(query).first()
        } else {
            val cached = cache[query]
            val baseResults = if (cached != null && cached.isFresh()) {
                cached.results
            } else {
                val result = searchLibraryUseCase(query, SEARCH_RESULT_LIMIT)
                result.getOrElse {
                    _uiState.value = SearchUiState.Error(it.message.orEmpty())
                    return
                }
            }
            val localTracks = localMediaRepository.searchTracks(query).first()
            val localAlbums = localMediaRepository.searchAlbums(query).first()
            val localArtists = localMediaRepository.searchArtists(query).first()
            SearchResults(
                albums = baseResults.albums.mergeWithLocal(localAlbums),
                artists = baseResults.artists.mergeWithLocal(localArtists),
                playlists = baseResults.playlists,
                tracks = baseResults.tracks.mergeWithLocal(localTracks)
            ).also { merged ->
                cache[query] = CachedResult(merged, System.currentTimeMillis())
            }
        }
        lastResults = results
        lastQuery = query
        val limits = sectionLimits.value
        _uiState.value = SearchUiState.Success(
            query = query,
            results = results,
            isEmpty = results.isEmpty(),
            albumLimit = limits.albumLimit,
            artistLimit = limits.artistLimit,
            playlistLimit = limits.playlistLimit,
            trackLimit = limits.trackLimit
        )
        if (!offline) {
            prefetchImages(results)
        }
    }

    private fun prefetchImages(results: SearchResults) {
        val urls = buildList {
            addAll(results.albums.mapNotNull { it.imageUrl })
            addAll(results.playlists.mapNotNull { it.imageUrl })
            addAll(results.artists.mapNotNull { it.imageUrl })
            addAll(results.tracks.mapNotNull { it.imageUrl })
        }.distinct().take(PREFETCH_IMAGE_COUNT)

        urls.forEach { url ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .build()
            )
        }
    }

    private fun SearchResults.isEmpty(): Boolean {
        return albums.isEmpty() && artists.isEmpty() && playlists.isEmpty() && tracks.isEmpty()
    }

    private data class CachedResult(
        val results: SearchResults,
        val timestampMs: Long
    ) {
        fun isFresh(): Boolean {
            return System.currentTimeMillis() - timestampMs <= CACHE_TTL_MS
        }
    }

    private data class SectionLimits(
        val albumLimit: Int = INITIAL_RESULT_LIMIT,
        val artistLimit: Int = INITIAL_RESULT_LIMIT,
        val playlistLimit: Int = INITIAL_RESULT_LIMIT,
        val trackLimit: Int = INITIAL_RESULT_LIMIT
    )

    companion object {
        private const val SEARCH_RESULT_LIMIT = 50
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val INITIAL_RESULT_LIMIT = 20
        private const val SHOW_MORE_INCREMENT = 20
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
        private const val PREFETCH_IMAGE_COUNT = 10
    }
}
