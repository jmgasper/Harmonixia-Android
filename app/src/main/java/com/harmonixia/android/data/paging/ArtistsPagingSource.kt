package com.harmonixia.android.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.PagingStatsTracker
import kotlin.math.max

class ArtistsPagingSource(
    private val repository: MusicAssistantRepository,
    private val pageSize: Int = PAGE_SIZE,
    private val statsTracker: PagingStatsTracker,
    private val isOfflineMode: () -> Boolean
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
            return try {
                val requestSize = max(pageSize, DEFAULT_FULL_FETCH_PAGE_SIZE)
                val initialResult = repository.fetchArtists(0, 0)
                val initialArtists = initialResult.getOrDefault(emptyList())
                val artists = if (initialResult.isSuccess && initialArtists.isNotEmpty()) {
                    statsTracker.recordPageLoaded(initialArtists.size)
                    val remaining = fetchAllPages(
                        pageSize = requestSize,
                        startOffset = initialArtists.size,
                        onPageLoaded = statsTracker::recordPageLoaded
                    ) { offset, limit ->
                        repository.fetchArtists(limit, offset)
                    }
                    initialArtists + remaining
                } else {
                    fetchAllPages(
                        pageSize = requestSize,
                        onPageLoaded = statsTracker::recordPageLoaded
                    ) { offset, limit ->
                        repository.fetchArtists(limit, offset)
                    }
                }
                LoadResult.Page(
                    data = artists,
                    prevKey = null,
                    nextKey = null
                )
            } catch (error: Throwable) {
                LoadResult.Error(error)
            }
        }
        val offset = params.key ?: 0
        val limit = params.loadSize.coerceAtLeast(1)
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
