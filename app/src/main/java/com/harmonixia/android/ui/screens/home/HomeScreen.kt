package com.harmonixia.android.ui.screens.home

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.ui.components.AlbumGridStatic
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.OfflineModeBanner
import com.harmonixia.android.ui.navigation.MainScaffoldActions
import com.harmonixia.android.ui.screens.settings.SettingsTab
import com.harmonixia.android.ui.theme.rememberAdaptiveSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: (SettingsTab?) -> Unit,
    onAlbumClick: (Album) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    val configuration = LocalConfiguration.current
    val spacing = rememberAdaptiveSpacing()
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns by remember(windowSizeClass, configuration) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> if (isLandscape) 3 else 2
                WindowWidthSizeClass.Medium -> if (isLandscape) 4 else 3
                WindowWidthSizeClass.Expanded -> {
                    if (isLandscape) {
                        (configuration.screenWidthDp / 160).coerceIn(4, 8)
                    } else {
                        (configuration.screenWidthDp / 180).coerceIn(4, 6)
                    }
                }
                else -> 2
            }
        }
    }
    val horizontalPadding = spacing.large
    val listVerticalPadding = if (isLandscape) 12.dp else 16.dp
    val listPadding = PaddingValues(horizontal = horizontalPadding, vertical = listVerticalPadding)
    val gridMaxHeight = configuration.screenHeightDp.dp * if (isExpanded) 0.8f else 0.6f
    val artworkSize = if (isExpanded) 180.dp else 150.dp
    val sectionColumns = if (isExpanded) (columns / 2).coerceAtLeast(2) else columns
    val sectionSpacing = if (isExpanded) {
        if (isLandscape) 16.dp else 24.dp
    } else {
        if (isLandscape) 12.dp else 16.dp
    }
    val sectionHeaderStyle = when {
        isExpanded && isLandscape -> MaterialTheme.typography.titleLarge
        isExpanded -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.titleLarge
    }
    val sectionBodyStyle = if (isExpanded) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.bodyMedium
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_home)) },
                actions = {
                    MainScaffoldActions()
                    IconButton(onClick = { onNavigateToSettings(null) }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.action_open_settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            state = rememberPullToRefreshState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(spacing.medium))
                            Text(
                                text = stringResource(R.string.home_loading),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is HomeUiState.Error -> {
                    val message = if (
                        state.message.contains("http://", ignoreCase = true) ||
                        state.message.contains("Failed to connect", ignoreCase = true)
                    ) {
                        stringResource(R.string.home_error_load_failed)
                    } else {
                        state.message.ifBlank { stringResource(R.string.home_error_load_failed) }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.medium)
                        ) {
                            ErrorCard(message = message)
                            Button(onClick = viewModel::refresh) {
                                Text(text = stringResource(R.string.action_retry))
                            }
                        }
                    }
                }
                is HomeUiState.Offline -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.medium)
                        ) {
                            OfflineModeBanner(
                                text = stringResource(R.string.home_offline_title),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(spacing.small))
                            Icon(
                                imageVector = Icons.Outlined.WifiOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stringResource(R.string.home_offline_message),
                                style = sectionBodyStyle,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (
                                state.albumCount > 0 ||
                                state.artistCount > 0 ||
                                state.trackCount > 0
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.home_offline_stats,
                                        state.albumCount,
                                        state.artistCount,
                                        state.trackCount
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.home_offline_no_content),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                HomeUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.medium)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.home_empty_no_connection),
                                style = sectionBodyStyle,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is HomeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = listPadding,
                        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                    ) {
                        if (isExpanded) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                                ) {
                                    HomeAlbumSection(
                                        title = stringResource(R.string.home_recently_played),
                                        emptyText = stringResource(R.string.home_empty_recently_played),
                                        albums = state.recentlyPlayed,
                                        columns = sectionColumns,
                                        artworkSize = artworkSize,
                                        gridMaxHeight = gridMaxHeight,
                                        headerStyle = sectionHeaderStyle,
                                        bodyStyle = sectionBodyStyle,
                                        onAlbumClick = onAlbumClick,
                                        modifier = Modifier.weight(1f)
                                    )
                                    HomeAlbumSection(
                                        title = stringResource(R.string.home_recently_added),
                                        emptyText = stringResource(R.string.home_empty_recently_added),
                                        albums = state.recentlyAdded,
                                        columns = sectionColumns,
                                        artworkSize = artworkSize,
                                        gridMaxHeight = gridMaxHeight,
                                        headerStyle = sectionHeaderStyle,
                                        bodyStyle = sectionBodyStyle,
                                        onAlbumClick = onAlbumClick,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            item {
                                HomeAlbumSection(
                                    title = stringResource(R.string.home_recently_played),
                                    emptyText = stringResource(R.string.home_empty_recently_played),
                                    albums = state.recentlyPlayed,
                                    columns = columns,
                                    artworkSize = artworkSize,
                                    gridMaxHeight = gridMaxHeight,
                                    headerStyle = sectionHeaderStyle,
                                    bodyStyle = sectionBodyStyle,
                                    onAlbumClick = onAlbumClick,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                HomeAlbumSection(
                                    title = stringResource(R.string.home_recently_added),
                                    emptyText = stringResource(R.string.home_empty_recently_added),
                                    albums = state.recentlyAdded,
                                    columns = columns,
                                    artworkSize = artworkSize,
                                    gridMaxHeight = gridMaxHeight,
                                    headerStyle = sectionHeaderStyle,
                                    bodyStyle = sectionBodyStyle,
                                    onAlbumClick = onAlbumClick,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAlbumSection(
    title: String,
    emptyText: String,
    albums: List<Album>,
    columns: Int,
    artworkSize: androidx.compose.ui.unit.Dp,
    gridMaxHeight: androidx.compose.ui.unit.Dp,
    headerStyle: androidx.compose.ui.text.TextStyle,
    bodyStyle: androidx.compose.ui.text.TextStyle,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    Column(modifier = modifier) {
        Text(
            text = title,
            style = headerStyle
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        if (albums.isEmpty()) {
            Text(
                text = emptyText,
                style = bodyStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            AlbumGridStatic(
                albums = albums,
                onAlbumClick = onAlbumClick,
                columns = columns,
                artworkSize = artworkSize,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.heightIn(max = gridMaxHeight)
            )
        }
    }
}
