package com.harmonixia.android.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertFooterItem
import com.harmonixia.android.data.paging.AlbumsPagingSource
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.AlbumType
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val localMediaRepository: LocalMediaRepository,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager,
    private val pagingStatsTracker: PagingStatsTracker,
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

    private val _uiState = MutableStateFlow<AlbumsUiState>(AlbumsUiState.Loading)
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    private val defaultAlbumTypes = setOf(
        AlbumType.ALBUM,
        AlbumType.SINGLE,
        AlbumType.EP,
        AlbumType.COMPILATION,
        AlbumType.UNKNOWN
    )

    private val selectedAlbumTypes = MutableStateFlow(defaultAlbumTypes)

    private val pagingConfig = MutableStateFlow(
        PagingConfig(
            pageSize = AlbumsPagingSource.PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = true,
            initialLoadSize = AlbumsPagingSource.PAGE_SIZE
        )
    )

    private var pagingSource: AlbumsPagingSource? = null

    private val albumsFlowReset = MutableStateFlow(0)

    private val baseFlow: Flow<PagingData<Album>> = combine(
        pagingConfig,
        offlineModeChanges
    ) { config, offline ->
        config to offline
    }.flatMapLatest { (config, offline) ->
        if (offline) {
            flowOf(PagingData.empty())
        } else {
            Pager(config) {
                AlbumsPagingSource(
                    repository,
                    config.pageSize,
                    pagingStatsTracker,
                    isOfflineMode = { isOfflineMode.value }
                ).also { pagingSource = it }
            }.flow
        }
    }

    private val onlineAlbumsFlow: Flow<PagingData<Album>> = combine(
        selectedAlbumTypes,
        localMediaRepository.getAllAlbums(),
        offlineModeChanges
    ) { types, localAlbums, offline ->
        Triple(types, localAlbums, offline)
    }.flatMapLatest { (types, localAlbums, offline) ->
        if (offline || types.isEmpty()) {
            flowOf(PagingData.empty())
        } else {
            baseFlow.map { pagingData ->
                val filteredPaging = pagingData.filter { album -> types.contains(album.albumType) }
                val filteredLocal = localAlbums.filter { album -> types.contains(album.albumType) }
                mergePagingWithLocal(filteredPaging, filteredLocal)
            }
        }
    }

    private val localAlbumsFlow: Flow<PagingData<Album>> = combine(
        localMediaRepository.getAllAlbums(),
        selectedAlbumTypes
    ) { albums, types ->
        if (types.isEmpty()) {
            PagingData.empty()
        } else {
            PagingData.from(albums.filter { album -> types.contains(album.albumType) })
        }
    }

    private val mergedAlbumsFlow: Flow<PagingData<Album>> = combine(
        offlineModeChanges,
        albumsFlowReset
    ) { offline, _ ->
        offline
    }.transformLatest { offline ->
        if (offline) {
            emitAll(localAlbumsFlow)
        } else {
            emitAll(onlineAlbumsFlow)
        }
    }

    val albumsFlow: Flow<PagingData<Album>> = mergedAlbumsFlow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            offlineModeChanges.collect { offline ->
                if (offline) {
                    pagingSource?.invalidate()
                    albumsFlowReset.value = albumsFlowReset.value + 1
                    _uiState.value = AlbumsUiState.Success(
                        albums = localAlbumsFlow,
                        selectedAlbumTypes = selectedAlbumTypes.value
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(connectionState, offlineModeChanges) { state, offline ->
                if (offline) {
                    AlbumsUiState.Success(
                        albums = localAlbumsFlow,
                        selectedAlbumTypes = selectedAlbumTypes.value
                    )
                } else {
                    when (state) {
                        is ConnectionState.Connected -> AlbumsUiState.Success(
                            albums = albumsFlow,
                            selectedAlbumTypes = selectedAlbumTypes.value
                        )
                        is ConnectionState.Connecting -> AlbumsUiState.Loading
                        is ConnectionState.Disconnected -> AlbumsUiState.Empty
                        is ConnectionState.Error -> AlbumsUiState.Error(state.message)
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

    fun updatePagingConfig(pageSize: Int, prefetchDistance: Int) {
        val current = pagingConfig.value
        if (current.pageSize == pageSize && current.prefetchDistance == prefetchDistance) {
            return
        }
        pagingConfig.value = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance,
            enablePlaceholders = true,
            initialLoadSize = pageSize
        )
        pagingSource?.invalidate()
    }

    fun toggleAlbumTypeFilter(type: AlbumType) {
        val updated = if (selectedAlbumTypes.value.contains(type)) {
            selectedAlbumTypes.value - type
        } else {
            selectedAlbumTypes.value + type
        }
        selectedAlbumTypes.value = updated
        val currentState = _uiState.value
        if (currentState is AlbumsUiState.Success) {
            _uiState.value = currentState.copy(selectedAlbumTypes = updated)
        }
    }

    private fun mergePagingWithLocal(
        pagingData: PagingData<Album>,
        localAlbums: List<Album>
    ): PagingData<Album> {
        if (localAlbums.isEmpty()) return pagingData
        val remoteWithoutLocal = pagingData.filter { album ->
            val merged = listOf(album).mergeWithLocal(localAlbums)
            merged.firstOrNull()?.provider != OFFLINE_PROVIDER
        }
        return localAlbums.fold(remoteWithoutLocal) { acc, localAlbum ->
            acc.insertFooterItem(item = localAlbum)
        }
    }

    companion object {
        private const val PREFETCH_DISTANCE = 35
    }
}
