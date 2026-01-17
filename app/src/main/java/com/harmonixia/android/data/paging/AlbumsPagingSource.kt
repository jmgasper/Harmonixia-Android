package com.harmonixia.android.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.harmonixia.android.data.repository.AlbumCacheRepository
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.PagingStatsTracker

class AlbumsPagingSource(
    private val repository: MusicAssistantRepository,
    private val pageSize: Int = PAGE_SIZE,
    private val statsTracker: PagingStatsTracker,
    private val isOfflineMode: () -> Boolean,
    private val albumCacheRepository: AlbumCacheRepository
) : PagingSource<Int, Album>() {
    private val coalescingLoader = CoalescingPagingLoader(
        coalesceDelayMs = COALESCE_DELAY_MS,
        fetchPage = { offset, limit -> repository.fetchAlbums(limit, offset) },
        onPageLoaded = statsTracker::recordPageLoaded
    )

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Album> {
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
            val cachedPage = albumCacheRepository.getCachedPage(offset, limit)
            if (cachedPage != null && cachedPage.albums.isNotEmpty()) {
                statsTracker.recordPageLoaded(cachedPage.albums.size)
                albumCacheRepository.prefetchIfIdle()
                val nextKey = if (cachedPage.albums.size < limit) null else offset + limit
                return LoadResult.Page(
                    data = cachedPage.albums,
                    prevKey = null,
                    nextKey = nextKey
                )
            }
            return repository.fetchAlbums(limit, offset)
                .fold(
                    onSuccess = { albums ->
                        statsTracker.recordPageLoaded(albums.size)
                        albumCacheRepository.prefetchFromInitialPage(
                            initialAlbums = albums,
                            startOffset = albums.size,
                            force = true
                        )
                        val nextKey = if (albums.size < limit) null else offset + limit
                        LoadResult.Page(
                            data = albums,
                            prevKey = null,
                            nextKey = nextKey
                        )
                    },
                    onFailure = { error -> LoadResult.Error(error) }
                )
        }
        val offset = params.key ?: 0
        val limit = params.loadSize.coerceAtLeast(1)

        val cachedPage = albumCacheRepository.getCachedPage(offset, limit)
        if (cachedPage != null) {
            statsTracker.recordPageLoaded(cachedPage.albums.size)
            val prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0)
            val nextKey = if (cachedPage.albums.size < limit) null else offset + limit
            return LoadResult.Page(
                data = cachedPage.albums,
                prevKey = prevKey,
                nextKey = nextKey
            )
        }

        return coalescingLoader.load(offset, limit)
    }

    override fun getRefreshKey(state: PagingState<Int, Album>): Int? {
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
