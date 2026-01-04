package com.harmonixia.android.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.PagingStatsTracker
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AlbumsPagingSource(
    private val repository: MusicAssistantRepository,
    private val pageSize: Int = PAGE_SIZE,
    private val statsTracker: PagingStatsTracker,
    private val isOfflineMode: () -> Boolean
) : PagingSource<Int, Album>() {
    private val batchMutex = Mutex()
    private var pendingBatch: BatchRequest? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Album> {
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
                val initialResult = repository.fetchAlbums(0, 0)
                val initialAlbums = initialResult.getOrDefault(emptyList())
                val albums = if (initialResult.isSuccess && initialAlbums.isNotEmpty()) {
                    statsTracker.recordPageLoaded(initialAlbums.size)
                    val remaining = fetchAllPages(
                        pageSize = requestSize,
                        startOffset = initialAlbums.size,
                        onPageLoaded = statsTracker::recordPageLoaded
                    ) { offset, limit ->
                        repository.fetchAlbums(limit, offset)
                    }
                    initialAlbums + remaining
                } else {
                    fetchAllPages(
                        pageSize = requestSize,
                        onPageLoaded = statsTracker::recordPageLoaded
                    ) { offset, limit ->
                        repository.fetchAlbums(limit, offset)
                    }
                }
                LoadResult.Page(
                    data = albums,
                    prevKey = null,
                    nextKey = null
                )
            } catch (error: Throwable) {
                LoadResult.Error(error)
            }
        }
        val offset = params.key ?: 0
        val limit = params.loadSize.coerceAtLeast(1)

        val waiter = PendingRequest(offset, limit, CompletableDeferred())
        val batch = batchMutex.withLock {
            val now = System.currentTimeMillis()
            val currentBatch = pendingBatch
            if (currentBatch != null &&
                now - currentBatch.createdAtMillis <= COALESCE_DELAY_MS &&
                rangesOverlapOrAdjacent(currentBatch.offset, currentBatch.limit, offset, limit)
            ) {
                expandBatch(currentBatch, offset, limit)
                currentBatch.waiters.add(waiter)
                null
            } else {
                val newBatch = BatchRequest(offset, limit, now, mutableListOf(waiter))
                pendingBatch = newBatch
                newBatch
            }
        }

        if (batch == null) {
            return waiter.deferred.await()
        }

        delay(COALESCE_DELAY_MS)
        batchMutex.withLock {
            if (pendingBatch === batch) {
                pendingBatch = null
            }
        }
        processBatch(batch)
        return waiter.deferred.await()
    }

    override fun getRefreshKey(state: PagingState<Int, Album>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val closestPage = state.closestPageToPosition(anchorPosition)
        return closestPage?.prevKey?.plus(pageSize)
            ?: closestPage?.nextKey?.minus(pageSize)
    }

    private suspend fun processBatch(batch: BatchRequest) {
        val result = try {
            repository.fetchAlbums(batch.limit, batch.offset)
        } catch (error: Throwable) {
            val loadResult = LoadResult.Error<Int, Album>(error)
            batch.waiters.forEach { waiter ->
                waiter.deferred.complete(loadResult)
            }
            return
        }
        result.fold(
            onSuccess = { albums ->
                statsTracker.recordPageLoaded(albums.size)
                batch.waiters.forEach { waiter ->
                    waiter.deferred.complete(
                        buildPage(albums, batch.offset, waiter.offset, waiter.limit)
                    )
                }
            },
            onFailure = { error ->
                val loadResult = LoadResult.Error<Int, Album>(error)
                batch.waiters.forEach { waiter ->
                    waiter.deferred.complete(loadResult)
                }
            }
        )
    }

    private fun buildPage(
        albums: List<Album>,
        batchOffset: Int,
        offset: Int,
        limit: Int
    ): LoadResult.Page<Int, Album> {
        val relativeStart = (offset - batchOffset).coerceAtLeast(0)
        val relativeEnd = (relativeStart + limit).coerceAtMost(albums.size)
        val pageData = if (relativeStart >= albums.size) {
            emptyList()
        } else {
            albums.subList(relativeStart, relativeEnd).toList()
        }
        val prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0)
        val nextKey = if (pageData.size < limit) null else offset + limit
        return LoadResult.Page(
            data = pageData,
            prevKey = prevKey,
            nextKey = nextKey
        )
    }

    private fun rangesOverlapOrAdjacent(
        batchOffset: Int,
        batchLimit: Int,
        offset: Int,
        limit: Int
    ): Boolean {
        val batchStart = batchOffset
        val batchEnd = batchOffset + batchLimit
        val requestStart = offset
        val requestEnd = offset + limit
        return requestStart <= batchEnd && requestEnd >= batchStart
    }

    private fun expandBatch(batch: BatchRequest, offset: Int, limit: Int) {
        val batchStart = batch.offset
        val batchEnd = batch.offset + batch.limit
        val requestStart = offset
        val requestEnd = offset + limit
        val newStart = min(batchStart, requestStart)
        val newEnd = max(batchEnd, requestEnd)
        batch.offset = newStart
        batch.limit = newEnd - newStart
    }

    private data class PendingRequest(
        val offset: Int,
        val limit: Int,
        val deferred: CompletableDeferred<LoadResult<Int, Album>>
    )

    private data class BatchRequest(
        var offset: Int,
        var limit: Int,
        val createdAtMillis: Long,
        val waiters: MutableList<PendingRequest>
    )

    companion object {
        const val PAGE_SIZE = 150
        private const val COALESCE_DELAY_MS = 75L
    }
}
