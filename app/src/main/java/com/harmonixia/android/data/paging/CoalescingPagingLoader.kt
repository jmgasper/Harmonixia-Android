package com.harmonixia.android.data.paging

import androidx.paging.PagingSource.LoadResult
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class CoalescingPagingLoader<T : Any>(
    private val coalesceDelayMs: Long,
    private val fetchPage: suspend (offset: Int, limit: Int) -> Result<List<T>>,
    private val onPageLoaded: (Int) -> Unit = {}
) {
    private val batchMutex = Mutex()
    private var pendingBatch: BatchRequest<T>? = null

    suspend fun load(offset: Int, limit: Int): LoadResult<Int, T> {
        val waiter = PendingRequest(
            offset,
            limit,
            CompletableDeferred<LoadResult<Int, T>>()
        )
        val batch = batchMutex.withLock {
            val now = System.currentTimeMillis()
            val currentBatch = pendingBatch
            if (currentBatch != null &&
                now - currentBatch.createdAtMillis <= coalesceDelayMs &&
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

        delay(coalesceDelayMs)
        batchMutex.withLock {
            if (pendingBatch === batch) {
                pendingBatch = null
            }
        }
        processBatch(batch)
        return waiter.deferred.await()
    }

    private suspend fun processBatch(batch: BatchRequest<T>) {
        val result = try {
            fetchPage(batch.offset, batch.limit)
        } catch (error: Throwable) {
            Result.failure(error)
        }
        result.fold(
            onSuccess = { items ->
                onPageLoaded(items.size)
                batch.waiters.forEach { waiter ->
                    waiter.deferred.complete(
                        buildPage(items, batch.offset, waiter.offset, waiter.limit)
                    )
                }
            },
            onFailure = { error ->
                val loadResult = LoadResult.Error<Int, T>(error)
                batch.waiters.forEach { waiter ->
                    waiter.deferred.complete(loadResult)
                }
            }
        )
    }

    private fun buildPage(
        items: List<T>,
        batchOffset: Int,
        offset: Int,
        limit: Int
    ): LoadResult.Page<Int, T> {
        val relativeStart = (offset - batchOffset).coerceAtLeast(0)
        val relativeEnd = (relativeStart + limit).coerceAtMost(items.size)
        val pageData = if (relativeStart >= items.size) {
            emptyList()
        } else {
            items.subList(relativeStart, relativeEnd).toList()
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

    private fun expandBatch(batch: BatchRequest<T>, offset: Int, limit: Int) {
        val batchStart = batch.offset
        val batchEnd = batch.offset + batch.limit
        val requestStart = offset
        val requestEnd = offset + limit
        val newStart = min(batchStart, requestStart)
        val newEnd = max(batchEnd, requestEnd)
        batch.offset = newStart
        batch.limit = newEnd - newStart
    }

    private data class PendingRequest<T : Any>(
        val offset: Int,
        val limit: Int,
        val deferred: CompletableDeferred<LoadResult<Int, T>>
    )

    private data class BatchRequest<T : Any>(
        var offset: Int,
        var limit: Int,
        val createdAtMillis: Long,
        val waiters: MutableList<PendingRequest<T>>
    )
}
