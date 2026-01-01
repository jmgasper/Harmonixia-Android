package com.harmonixia.android.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.PagingStatsTracker
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaylistsPagingSource(
    private val repository: MusicAssistantRepository,
    private val pageSize: Int = PAGE_SIZE,
    private val statsTracker: PagingStatsTracker,
    private val isOfflineMode: () -> Boolean
) : PagingSource<Int, Playlist>() {
    private val batchMutex = Mutex()
    private var pendingBatch: BatchRequest? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Playlist> {
        if (isOfflineMode()) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null
            )
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

    override fun getRefreshKey(state: PagingState<Int, Playlist>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val closestPage = state.closestPageToPosition(anchorPosition)
        return closestPage?.prevKey?.plus(pageSize)
            ?: closestPage?.nextKey?.minus(pageSize)
    }

    private suspend fun processBatch(batch: BatchRequest) {
        val result = try {
            repository.fetchPlaylists(batch.limit, batch.offset)
        } catch (error: Throwable) {
            val loadResult = LoadResult.Error<Int, Playlist>(error)
            batch.waiters.forEach { waiter ->
                waiter.deferred.complete(loadResult)
            }
            return
        }
        result.fold(
            onSuccess = { playlists ->
                statsTracker.recordPageLoaded(playlists.size)
                batch.waiters.forEach { waiter ->
                    waiter.deferred.complete(
                        buildPage(playlists, batch.offset, waiter.offset, waiter.limit)
                    )
                }
            },
            onFailure = { error ->
                val loadResult = LoadResult.Error<Int, Playlist>(error)
                batch.waiters.forEach { waiter ->
                    waiter.deferred.complete(loadResult)
                }
            }
        )
    }

    private fun buildPage(
        playlists: List<Playlist>,
        batchOffset: Int,
        offset: Int,
        limit: Int
    ): LoadResult.Page<Int, Playlist> {
        val relativeStart = (offset - batchOffset).coerceAtLeast(0)
        val relativeEnd = (relativeStart + limit).coerceAtMost(playlists.size)
        val pageData = if (relativeStart >= playlists.size) {
            emptyList()
        } else {
            playlists.subList(relativeStart, relativeEnd).toList()
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
        val deferred: CompletableDeferred<LoadResult<Int, Playlist>>
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
