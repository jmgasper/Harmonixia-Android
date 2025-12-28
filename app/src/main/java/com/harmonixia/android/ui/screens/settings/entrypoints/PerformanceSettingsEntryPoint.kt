package com.harmonixia.android.ui.screens.settings.entrypoints

import coil3.ImageLoader
import com.harmonixia.android.util.PagingStatsTracker
import com.harmonixia.android.util.PerformanceMonitor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PerformanceSettingsEntryPoint {
    fun performanceMonitor(): PerformanceMonitor
    fun pagingStatsTracker(): PagingStatsTracker
    fun imageLoader(): ImageLoader
}
