package com.harmonixia.android.ui.screens.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.harmonixia.android.data.paging.ArtistsPagingSource
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.util.NetworkConnectivityManager
import com.harmonixia.android.util.PagingStatsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val downloadRepository: DownloadRepository,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager,
    private val pagingStatsTracker: PagingStatsTracker
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

    private val _uiState = MutableStateFlow<ArtistsUiState>(ArtistsUiState.Loading)
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    private val _artistAlbumCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val artistAlbumCounts: StateFlow<Map<String, Int>> = _artistAlbumCounts.asStateFlow()

    private var pagingSource: ArtistsPagingSource? = null

    private val onlineArtistsFlow: Flow<PagingData<Artist>> = Pager(
        PagingConfig(
            pageSize = ArtistsPagingSource.PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = true,
            initialLoadSize = ArtistsPagingSource.PAGE_SIZE
        )
    ) {
        ArtistsPagingSource(repository, ArtistsPagingSource.PAGE_SIZE, pagingStatsTracker).also {
            pagingSource = it
        }
    }.flow

    private val offlineArtistsFlow: Flow<PagingData<Artist>> =
        downloadRepository.getDownloadedAlbums().map { albums ->
            PagingData.from(buildOfflineArtists(albums))
        }

    val artistsFlow: Flow<PagingData<Artist>> = isOfflineMode
        .flatMapLatest { offline ->
            if (offline) offlineArtistsFlow else onlineArtistsFlow
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            isOfflineMode.collect { offline ->
                if (offline) {
                    pagingSource?.invalidate()
                    _artistAlbumCounts.value = emptyMap()
                }
            }
        }
        viewModelScope.launch {
            combine(connectionState, isOfflineMode) { state, offline ->
                if (offline) {
                    ArtistsUiState.Success(artistsFlow)
                } else {
                    when (state) {
                        is ConnectionState.Connected -> ArtistsUiState.Success(artistsFlow)
                        is ConnectionState.Connecting -> ArtistsUiState.Loading
                        is ConnectionState.Disconnected -> ArtistsUiState.Empty
                        is ConnectionState.Error -> ArtistsUiState.Error(state.message)
                    }
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun refresh() {
        pagingSource?.invalidate()
    }

    fun loadAlbumCount(artist: Artist) {
        val key = artistKey(artist)
        if (_artistAlbumCounts.value.containsKey(key)) return
        viewModelScope.launch {
            val count = fetchArtistAlbumCount(artist)
            _artistAlbumCounts.update { current ->
                if (current.containsKey(key)) current else current + (key to count)
            }
        }
    }

    private suspend fun fetchArtistAlbumCount(artist: Artist): Int {
        val targetName = normalizeName(artist.name)
        if (targetName.isBlank()) return 0
        if (isOfflineMode.value) {
            val albums = downloadRepository.getDownloadedAlbums().first()
            return albums.count { album ->
                album.artists.any { name -> normalizeName(name) == targetName }
            }
        }
        return runCatching {
            var offset = 0
            var count = 0
            while (true) {
                val page = repository.fetchAlbums(ALBUM_LIST_LIMIT, offset).getOrThrow()
                if (page.isEmpty()) break
                count += page.count { album ->
                    album.artists.any { name -> normalizeName(name) == targetName }
                }
                if (page.size < ALBUM_LIST_LIMIT) break
                offset += ALBUM_LIST_LIMIT
            }
            count
        }.getOrDefault(0)
    }

    private fun normalizeName(name: String?): String {
        return name?.trim()?.lowercase().orEmpty()
    }

    private fun artistKey(artist: Artist): String {
        return "${artist.provider}:${artist.itemId}"
    }

    private fun buildOfflineArtists(albums: List<com.harmonixia.android.domain.model.Album>): List<Artist> {
        val artistsByName = LinkedHashMap<String, Artist>()
        for (album in albums) {
            val imageUrl = album.imageUrl
            for (artistName in album.artists) {
                val trimmed = artistName.trim()
                val normalized = normalizeName(trimmed)
                if (normalized.isBlank()) continue
                if (artistsByName.containsKey(normalized)) continue
                val encodedId = Uri.encode(trimmed)
                artistsByName[normalized] = Artist(
                    itemId = encodedId,
                    provider = OFFLINE_PROVIDER,
                    uri = "offline:artist:$encodedId",
                    name = trimmed,
                    sortName = trimmed.lowercase(),
                    imageUrl = imageUrl
                )
            }
        }
        return artistsByName.values.sortedBy { it.name.lowercase() }
    }

    companion object {
        private const val PREFETCH_DISTANCE = 15
        private const val ALBUM_LIST_LIMIT = 200
    }
}
