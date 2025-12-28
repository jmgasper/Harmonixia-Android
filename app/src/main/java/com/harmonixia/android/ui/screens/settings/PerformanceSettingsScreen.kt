package com.harmonixia.android.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.harmonixia.android.R
import com.harmonixia.android.ui.screens.settings.entrypoints.PerformanceSettingsEntryPoint
import dagger.hilt.android.EntryPointAccessors

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PerformanceSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, PerformanceSettingsEntryPoint::class.java)
    }
    val performanceMonitor = entryPoint.performanceMonitor()
    val pagingStatsTracker = entryPoint.pagingStatsTracker()
    val imageLoader = entryPoint.imageLoader()
    val pagingStats = pagingStatsTracker.stats.collectAsStateWithLifecycle().value

    val averageLatency = performanceMonitor.getAveragePlaybackLatency()
    val cacheHitRateLabel = stringResource(R.string.performance_settings_cache_hit_rate_unavailable)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.performance_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.performance_settings_playback_latency,
                    averageLatency
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = cacheHitRateLabel,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(
                    R.string.performance_settings_memory_cache,
                    formatBytes(imageLoader.memoryCache?.size),
                    formatBytes(imageLoader.memoryCache?.maxSize)
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(
                    R.string.performance_settings_disk_cache,
                    formatBytes(imageLoader.diskCache?.size),
                    formatBytes(imageLoader.diskCache?.maxSize)
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { clearImageCaches(imageLoader) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.performance_settings_clear_image_cache))
            }
            Button(
                onClick = {
                    clearImageCaches(imageLoader)
                    pagingStatsTracker.reset()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.performance_settings_clear_all_caches))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.performance_settings_paging_items,
                    pagingStats.itemsLoaded
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(
                    R.string.performance_settings_paging_pages,
                    pagingStats.pagesLoaded
                ),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun clearImageCaches(imageLoader: ImageLoader) {
    imageLoader.memoryCache?.clear()
    imageLoader.diskCache?.clear()
}

private fun formatBytes(value: Long?): String {
    val bytes = value ?: 0L
    val mb = bytes / (1024f * 1024f)
    return String.format("%.1f MB", mb)
}
