package com.harmonixia.android.ui.screens.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertFooterItem
import com.harmonixia.android.data.paging.ArtistsPagingSource
import com.harmonixia.android.data.repository.ArtistCacheRepository
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.NetworkConnectivityManager
import com.harmonixia.android.util.PagingStatsTracker
import com.harmonixia.android.util.mergeWithLocal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val localMediaRepository: LocalMediaRepository,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager,
    private val pagingStatsTracker: PagingStatsTracker,
    private val artistCacheRepository: ArtistCacheRepository,
    val imageQualityManager: ImageQualityManager
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
    private val offlineModeChanges = isOfflineMode

    private val _uiState = MutableStateFlow<ArtistsUiState>(ArtistsUiState.Loading)
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    private val _artistAlbumCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val artistAlbumCounts: StateFlow<Map<String, Int>> = _artistAlbumCounts.asStateFlow()

    private var pagingSource: ArtistsPagingSource? = null

    private val artistsFlowReset = MutableStateFlow(0)

    private fun artistPagerFlow(isOffline: Boolean): Flow<PagingData<Artist>> {
        if (isOffline) {
            return flowOf(PagingData.empty())
        }
        return Pager(
            PagingConfig(
                pageSize = ArtistsPagingSource.PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = true,
                initialLoadSize = ArtistsPagingSource.PAGE_SIZE
            )
        ) {
            ArtistsPagingSource(
                repository,
                ArtistsPagingSource.PAGE_SIZE,
                pagingStatsTracker,
                isOfflineMode = { isOfflineMode.value },
                artistCacheRepository = artistCacheRepository
            ).also { pagingSource = it }
        }.flow
    }

    private val onlineArtistsFlow: Flow<PagingData<Artist>> = combine(
        localMediaRepository.getAllArtists(),
        offlineModeChanges
    ) { localArtists, offline ->
        localArtists to offline
    }.flatMapLatest { (localArtists, offline) ->
        val pagingFlow = artistPagerFlow(offline)
        if (offline) {
            pagingFlow
        } else {
            pagingFlow.map { pagingData ->
                mergePagingWithLocal(pagingData, localArtists)
            }
        }
    }

    private val localArtistsFlow: Flow<PagingData<Artist>> =
        localMediaRepository.getAllArtists().map { artists ->
            PagingData.from(artists)
        }

    private val mergedArtistsFlow: Flow<PagingData<Artist>> = combine(
        offlineModeChanges,
        artistsFlowReset
    ) { offline, _ ->
        offline
    }.transformLatest { offline ->
        if (offline) {
            emitAll(localArtistsFlow)
        } else {
            emitAll(onlineArtistsFlow)
        }
    }

    val artistsFlow: Flow<PagingData<Artist>> = mergedArtistsFlow.cachedIn(viewModelScope)

    val cachedArtists: StateFlow<List<Artist>> = combine(
        artistCacheRepository.observeCachedArtists(),
        localMediaRepository.getAllArtists(),
        offlineModeChanges
    ) { cached, localArtists, offline ->
        if (offline) {
            emptyList()
        } else {
            cached.mergeWithLocal(localArtists)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isArtistCacheComplete: StateFlow<Boolean> = artistCacheRepository.isCacheComplete

    init {
        viewModelScope.launch {
            offlineModeChanges.collect { offline ->
                if (offline) {
                    pagingSource?.invalidate()
                    _artistAlbumCounts.value = emptyMap()
                    artistsFlowReset.value = artistsFlowReset.value + 1
                    _uiState.value = ArtistsUiState.Success(localArtistsFlow)
                } else {
                    artistCacheRepository.prefetchIfIdle()
                }
            }
        }
        viewModelScope.launch {
            combine(connectionState, offlineModeChanges) { state, offline ->
                if (offline) {
                    ArtistsUiState.Success(localArtistsFlow)
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
        if (!isOfflineMode.value) {
            artistCacheRepository.refresh()
        }
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

    private fun mergePagingWithLocal(
        pagingData: PagingData<Artist>,
        localArtists: List<Artist>
    ): PagingData<Artist> {
        if (localArtists.isEmpty()) return pagingData
        val remoteWithoutLocal = pagingData.filter { artist ->
            val merged = listOf(artist).mergeWithLocal(localArtists)
            merged.firstOrNull()?.provider != OFFLINE_PROVIDER
        }
        return localArtists.fold(remoteWithoutLocal) { acc, localArtist ->
            acc.insertFooterItem(item = localArtist)
        }
    }

    companion object {
        private const val PREFETCH_DISTANCE = 15
        private const val ALBUM_LIST_LIMIT = 200
    }
}
