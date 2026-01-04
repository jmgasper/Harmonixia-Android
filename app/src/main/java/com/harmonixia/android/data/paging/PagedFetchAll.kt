package com.harmonixia.android.data.paging

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal const val DEFAULT_FULL_FETCH_PAGE_SIZE = 200
internal const val DEFAULT_FULL_FETCH_PARALLELISM = 3

internal suspend fun <T> fetchAllPages(
    pageSize: Int = DEFAULT_FULL_FETCH_PAGE_SIZE,
    parallelism: Int = DEFAULT_FULL_FETCH_PARALLELISM,
    startOffset: Int = 0,
    onPageLoaded: (Int) -> Unit = {},
    fetchPage: suspend (offset: Int, limit: Int) -> Result<List<T>>
): List<T> = coroutineScope {
    val safePageSize = pageSize.coerceAtLeast(1)
    val safeParallelism = parallelism.coerceAtLeast(1)
    val safeStartOffset = startOffset.coerceAtLeast(0)
    val pages = ConcurrentHashMap<Int, List<T>>()
    val nextOffset = AtomicInteger(safeStartOffset)
    val stopOffset = AtomicInteger(Int.MAX_VALUE)

    suspend fun worker() {
        while (true) {
            val offset = nextOffset.getAndAdd(safePageSize)
            if (offset >= stopOffset.get()) return
            val page = fetchPage(offset, safePageSize).getOrThrow()
            if (page.isEmpty()) {
                updateStopOffset(stopOffset, offset)
                return
            }
            pages[offset] = page
            onPageLoaded(page.size)
            if (page.size < safePageSize) {
                updateStopOffset(stopOffset, offset + page.size)
                return
            }
        }
    }

    val jobs = List(safeParallelism) { async { worker() } }
    jobs.awaitAll()
    val maxOffset = stopOffset.get()
    pages.toSortedMap().asSequence()
        .filter { (offset, _) -> offset < maxOffset }
        .flatMap { (_, items) -> items.asSequence() }
        .toList()
}

private fun updateStopOffset(stopOffset: AtomicInteger, newValue: Int) {
    while (true) {
        val current = stopOffset.get()
        if (newValue >= current) return
        if (stopOffset.compareAndSet(current, newValue)) return
    }
}
