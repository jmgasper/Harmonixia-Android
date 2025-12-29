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
        val limit = maxOf(pageSize, FETCH_PAGE_SIZE).coerceAtLeast(1)
        return runCatching {
            val albums = mutableListOf<Album>()
            var offset = 0
            while (true) {
                val page = repository.fetchAlbums(limit, offset).getOrThrow()
                if (page.isEmpty()) break
                statsTracker.recordPageLoaded(page.size)
                albums.addAll(page)
                if (page.size < limit) break
                offset += limit
            }
            LoadResult.Page<Int, Album>(
                data = albums,
                prevKey = null,
                nextKey = null
            )
        }.getOrElse { error -> LoadResult.Error(error) }
    }

    override fun getRefreshKey(state: PagingState<Int, Album>): Int? {
        return null
    }

    companion object {
        const val PAGE_SIZE = 100
        private const val FETCH_PAGE_SIZE = 200
    }
}
