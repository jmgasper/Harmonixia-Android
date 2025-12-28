package com.harmonixia.android.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.harmonixia.android.data.paging.AlbumsPagingSource
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.AlbumType
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.util.PagingStatsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    private val pagingStatsTracker: PagingStatsTracker
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = getConnectionStateUseCase()

    private val _uiState = MutableStateFlow<AlbumsUiState>(AlbumsUiState.Loading)
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    private val defaultAlbumTypes = setOf(
        AlbumType.ALBUM,
        AlbumType.SINGLE,
        AlbumType.EP,
        AlbumType.COMPILATION
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

    private val baseFlow: Flow<PagingData<Album>> = pagingConfig
        .flatMapLatest { config ->
            Pager(config) {
                AlbumsPagingSource(repository, config.pageSize, pagingStatsTracker).also {
                    pagingSource = it
                }
            }.flow
        }

    val albumsFlow: Flow<PagingData<Album>> = combine(baseFlow, selectedAlbumTypes) {
            pagingData,
            types
        ->
        if (types.isEmpty()) {
            PagingData.empty()
        } else {
            pagingData.filter { album -> types.contains(album.albumType) }
        }
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            connectionState.collect { state ->
                _uiState.value = when (state) {
                    is ConnectionState.Connected -> AlbumsUiState.Success(
                        albums = albumsFlow,
                        selectedAlbumTypes = selectedAlbumTypes.value
                    )
                    is ConnectionState.Connecting -> AlbumsUiState.Loading
                    is ConnectionState.Disconnected -> AlbumsUiState.Empty
                    is ConnectionState.Error -> AlbumsUiState.Error(state.message)
                }
            }
        }
    }

    fun refresh() {
        pagingSource?.invalidate()
    }

    fun updatePagingConfig(pageSize: Int, prefetchDistance: Int) {
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

    companion object {
        private const val PREFETCH_DISTANCE = 15
    }
}
