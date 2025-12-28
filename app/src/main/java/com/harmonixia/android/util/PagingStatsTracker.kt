package com.harmonixia.android.util

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class PagingStatsTracker @Inject constructor() {
    private val _stats = MutableStateFlow(PagingStats())
    val stats: StateFlow<PagingStats> = _stats.asStateFlow()

    fun recordPageLoaded(itemCount: Int) {
        _stats.update { current ->
            current.copy(
                pagesLoaded = current.pagesLoaded + 1,
                itemsLoaded = current.itemsLoaded + itemCount
            )
        }
    }

    fun reset() {
        _stats.value = PagingStats()
    }
}

data class PagingStats(
    val pagesLoaded: Int = 0,
    val itemsLoaded: Int = 0
)
