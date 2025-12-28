package com.harmonixia.android.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.PagingStatsTracker

class AlbumsPagingSource(
    private val repository: MusicAssistantRepository,
    private val pageSize: Int = PAGE_SIZE,
    private val statsTracker: PagingStatsTracker
) : PagingSource<Int, Album>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Album> {
        val offset = params.key ?: 0
        val limit = params.loadSize.coerceAtLeast(1)
        return repository.fetchAlbums(limit, offset)
            .fold(
                onSuccess = { albums ->
                    statsTracker.recordPageLoaded(albums.size)
                    val prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0)
                    val nextKey = if (albums.size < limit) null else offset + limit
                    LoadResult.Page(
                        data = albums,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                },
                onFailure = { error -> LoadResult.Error(error) }
            )
    }

    override fun getRefreshKey(state: PagingState<Int, Album>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val closestPage = state.closestPageToPosition(anchorPosition)
        return closestPage?.prevKey?.plus(pageSize)
            ?: closestPage?.nextKey?.minus(pageSize)
    }

    companion object {
        const val PAGE_SIZE = 100
    }
}
