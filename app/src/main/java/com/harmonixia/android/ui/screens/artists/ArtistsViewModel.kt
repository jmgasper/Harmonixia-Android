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
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.util.PagingStatsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    private val pagingStatsTracker: PagingStatsTracker
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = getConnectionStateUseCase()

    private val _uiState = MutableStateFlow<ArtistsUiState>(ArtistsUiState.Loading)
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    private val _artistAlbumCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val artistAlbumCounts: StateFlow<Map<String, Int>> = _artistAlbumCounts.asStateFlow()

    private var pagingSource: ArtistsPagingSource? = null

    val artistsFlow: Flow<PagingData<Artist>> = Pager(
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
    }.flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            connectionState.collect { state ->
                _uiState.value = when (state) {
                    is ConnectionState.Connected -> ArtistsUiState.Success(artistsFlow)
                    is ConnectionState.Connecting -> ArtistsUiState.Loading
                    is ConnectionState.Disconnected -> ArtistsUiState.Empty
                    is ConnectionState.Error -> ArtistsUiState.Error(state.message)
                }
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

    companion object {
        private const val PREFETCH_DISTANCE = 15
        private const val ALBUM_LIST_LIMIT = 200
    }
}
