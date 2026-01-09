package com.harmonixia.android.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.harmonixia.android.data.repository.ArtistCacheRepository
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.PagingStatsTracker

class ArtistsPagingSource(
    private val repository: MusicAssistantRepository,
    private val pageSize: Int = PAGE_SIZE,
    private val statsTracker: PagingStatsTracker,
    private val isOfflineMode: () -> Boolean,
    private val artistCacheRepository: ArtistCacheRepository
) : PagingSource<Int, Artist>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artist> {
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
            val cachedPage = artistCacheRepository.getCachedPage(offset, limit)
            if (cachedPage != null && cachedPage.artists.isNotEmpty()) {
                statsTracker.recordPageLoaded(cachedPage.artists.size)
                artistCacheRepository.prefetchIfIdle()
                val nextKey = if (cachedPage.artists.size < limit) null else offset + limit
                return LoadResult.Page(
                    data = cachedPage.artists,
                    prevKey = null,
                    nextKey = nextKey
                )
            }
            return repository.fetchArtists(limit, offset)
                .fold(
                    onSuccess = { artists ->
                        statsTracker.recordPageLoaded(artists.size)
                        artistCacheRepository.prefetchFromInitialPage(
                            initialArtists = artists,
                            startOffset = artists.size,
                            force = true
                        )
                        val nextKey = if (artists.size < limit) null else offset + limit
                        LoadResult.Page(
                            data = artists,
                            prevKey = null,
                            nextKey = nextKey
                        )
                    },
                    onFailure = { error -> LoadResult.Error(error) }
                )
        }
        val offset = params.key ?: 0
        val limit = params.loadSize.coerceAtLeast(1)

        val cachedPage = artistCacheRepository.getCachedPage(offset, limit)
        if (cachedPage != null) {
            statsTracker.recordPageLoaded(cachedPage.artists.size)
            val prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0)
            val nextKey = if (cachedPage.artists.size < limit) null else offset + limit
            return LoadResult.Page(
                data = cachedPage.artists,
                prevKey = prevKey,
                nextKey = nextKey
            )
        }
        return repository.fetchArtists(limit, offset)
            .fold(
                onSuccess = { artists ->
                    statsTracker.recordPageLoaded(artists.size)
                    val prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0)
                    val nextKey = if (artists.size < limit) null else offset + limit
                    LoadResult.Page(
                        data = artists,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                },
                onFailure = { error -> LoadResult.Error(error) }
            )
    }

    override fun getRefreshKey(state: PagingState<Int, Artist>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val closestPage = state.closestPageToPosition(anchorPosition)
        return closestPage?.prevKey?.plus(pageSize)
            ?: closestPage?.nextKey?.minus(pageSize)
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
