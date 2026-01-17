package com.harmonixia.android.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.PagingStatsTracker

class PlaylistsPagingSource(
    private val repository: MusicAssistantRepository,
    private val pageSize: Int = PAGE_SIZE,
    private val statsTracker: PagingStatsTracker,
    private val isOfflineMode: () -> Boolean
) : PagingSource<Int, Playlist>() {
    private val coalescingLoader = CoalescingPagingLoader(
        coalesceDelayMs = COALESCE_DELAY_MS,
        fetchPage = { offset, limit -> repository.fetchPlaylists(limit, offset) },
        onPageLoaded = statsTracker::recordPageLoaded
    )

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Playlist> {
        if (isOfflineMode()) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null
            )
        }
        if (params is LoadParams.Refresh) {
            val offset = 0
            val limit = params.loadSize.coerceAtLeast(1)
            return repository.fetchPlaylists(limit, offset)
                .fold(
                    onSuccess = { playlists ->
                        statsTracker.recordPageLoaded(playlists.size)
                        val nextKey = if (playlists.size < limit) null else offset + limit
                        LoadResult.Page(
                            data = playlists,
                            prevKey = null,
                            nextKey = nextKey
                        )
                    },
                    onFailure = { error -> LoadResult.Error(error) }
                )
        }
        val offset = params.key ?: 0
        val limit = params.loadSize.coerceAtLeast(1)

        return coalescingLoader.load(offset, limit)
    }

    override fun getRefreshKey(state: PagingState<Int, Playlist>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val closestPage = state.closestPageToPosition(anchorPosition)
        return closestPage?.prevKey?.plus(pageSize)
            ?: closestPage?.nextKey?.minus(pageSize)
    }

    companion object {
        const val PAGE_SIZE = 150
        private const val COALESCE_DELAY_MS = 75L
    }
}
