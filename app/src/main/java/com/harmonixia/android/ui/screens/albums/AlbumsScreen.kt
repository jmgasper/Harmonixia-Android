package com.harmonixia.android.ui.screens.albums

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.AlbumType
import com.harmonixia.android.ui.components.AlphabetFastScroller
import com.harmonixia.android.ui.components.AlbumGrid
import com.harmonixia.android.ui.components.AlbumListItem
import com.harmonixia.android.ui.components.AlbumListItemPlaceholder
import com.harmonixia.android.ui.components.AlbumTypeFilterMenu
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.OfflineModeBanner
import com.harmonixia.android.ui.components.alphabetIndexMap
import com.harmonixia.android.ui.navigation.MainScaffoldActions
import com.harmonixia.android.ui.screens.settings.SettingsTab
import com.harmonixia.android.ui.theme.rememberAdaptiveSpacing
import com.harmonixia.android.ui.util.buildAlbumArtworkRequest
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.LibraryViewMode
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onNavigateToSettings: (SettingsTab?) -> Unit,
    onAlbumClick: (Album) -> Unit,
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val imageQualityManager = viewModel.imageQualityManager
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()

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
    val listVerticalPadding = if (isLandscape) spacing.medium * 0.75f else spacing.medium
    val listPadding = PaddingValues(horizontal = horizontalPadding, vertical = listVerticalPadding)
    val gridVerticalPadding = if (isLandscape) spacing.large * 0.75f else spacing.large
    val gridPadding = PaddingValues(horizontal = horizontalPadding, vertical = gridVerticalPadding)

    val pageSize by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 50
                WindowWidthSizeClass.Medium -> 100
                WindowWidthSizeClass.Expanded -> 150
                else -> 50
            }
        }
    }
    val prefetchDistance by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 15
                WindowWidthSizeClass.Medium -> 30
                WindowWidthSizeClass.Expanded -> 50
                else -> 15
            }
        }
    }
    LaunchedEffect(pageSize, prefetchDistance) {
        viewModel.updatePagingConfig(pageSize, prefetchDistance)
    }

    val density = LocalDensity.current.density
    val artworkSize by remember(windowSizeClass, density) {
        derivedStateOf {
            if (isExpanded) {
                val scale = density.coerceIn(1f, 1.3f)
                150.dp * scale
            } else {
                150.dp
            }
        }
    }

    val defaultTypes = remember {
        setOf(
            AlbumType.ALBUM,
            AlbumType.SINGLE,
            AlbumType.EP,
            AlbumType.COMPILATION,
            AlbumType.UNKNOWN
        )
    }
    val selectedTypes = (uiState as? AlbumsUiState.Success)?.selectedAlbumTypes ?: defaultTypes

    val successState = uiState as? AlbumsUiState.Success
    val lazyPagingItems = successState?.albums?.collectAsLazyPagingItems()
    val filteredCount = lazyPagingItems?.itemSnapshotList?.items?.size ?: 0
    val totalCount = lazyPagingItems?.itemCount ?: 0
    val scrollScope = rememberCoroutineScope()
    val isListView by remember(viewMode) {
        derivedStateOf { viewMode == LibraryViewMode.LIST }
    }
    val collapsedListState = rememberLazyListState()
    val expandedListState = rememberLazyListState()
    val collapsedGridState = rememberLazyGridState()
    val expandedGridState = rememberLazyGridState()
    val alphabetIndexMap by remember(lazyPagingItems) {
        derivedStateOf {
            lazyPagingItems?.alphabetIndexMap { album -> album.name } ?: emptyMap()
        }
    }
    val showAlphabetScroller by remember(lazyPagingItems) {
        derivedStateOf {
            lazyPagingItems?.itemSnapshotList?.items?.isNotEmpty() == true
        }
    }

    var showFilterMenu by remember { mutableStateOf(false) }
    val showFilterLabel by remember(windowSizeClass) {
        derivedStateOf { windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact }
    }

    val titleText = if (uiState is AlbumsUiState.Success) {
        stringResource(R.string.albums_filter_count, filteredCount, totalCount)
    } else {
        stringResource(R.string.albums_title)
    }

    var selectedAlbum by rememberSaveable(stateSaver = AlbumSaver) { mutableStateOf<Album?>(null) }
    val handleAlbumClick: (Album) -> Unit = { album ->
        if (isExpanded) {
            selectedAlbum = album
        } else {
            onAlbumClick(album)
        }
    }

    val latestIsExpanded by rememberUpdatedState(isExpanded)
    val latestSelectedAlbum by rememberUpdatedState(selectedAlbum)
    val latestOnAlbumClick by rememberUpdatedState(onAlbumClick)
    DisposableEffect(windowSizeClass.widthSizeClass) {
        val wasExpanded = isExpanded
        onDispose {
            if (wasExpanded && !latestIsExpanded) {
                latestSelectedAlbum?.let { latestOnAlbumClick(it) }
            }
        }
    }

    val isRefreshing = !isOfflineMode && lazyPagingItems?.loadState?.refresh is LoadState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = titleText) },
                actions = {
                    Box {
                        if (showFilterLabel) {
                            TextButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = stringResource(R.string.albums_filter)
                                )
                                Spacer(modifier = Modifier.width(spacing.medium))
                                Column {
                                    Text(text = stringResource(R.string.albums_filter))
                                    if (uiState is AlbumsUiState.Success) {
                                        Text(
                                            text = stringResource(
                                                R.string.albums_filter_count,
                                                filteredCount,
                                                totalCount
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = stringResource(R.string.albums_filter)
                                )
                            }
                        }
                        if (showFilterMenu) {
                            AlbumTypeFilterMenu(
                                selectedTypes = selectedTypes,
                                onTypeToggle = viewModel::toggleAlbumTypeFilter,
                                onDismiss = { showFilterMenu = false }
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val nextMode = if (isListView) {
                                LibraryViewMode.GRID
                            } else {
                                LibraryViewMode.LIST
                            }
                            viewModel.updateViewMode(nextMode)
                        }
                    ) {
                        Icon(
                            imageVector = if (isListView) {
                                Icons.Outlined.GridView
                            } else {
                                Icons.Outlined.ViewList
                            },
                            contentDescription = stringResource(
                                if (isListView) {
                                    R.string.content_desc_show_grid_view
                                } else {
                                    R.string.content_desc_show_list_view
                                }
                            )
                        )
                    }
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
        onRefresh = {
            lazyPagingItems?.refresh() ?: viewModel.refresh()
        },
        state = rememberPullToRefreshState(),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isOfflineMode) {
                OfflineModeBanner(
                    text = stringResource(R.string.offline_mode_active),
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding)
                        .padding(top = spacing.medium)
                )
                Spacer(modifier = Modifier.height(spacing.small))
            }
            when (val state = uiState) {
                AlbumsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(spacing.medium))
                            Text(
                                text = stringResource(R.string.albums_loading),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is AlbumsUiState.Error -> {
                    val message = state.message.ifBlank {
                        stringResource(R.string.albums_error)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = horizontalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.medium)
                        ) {
                            ErrorCard(message = message)
                            Button(onClick = { lazyPagingItems?.retry() }) {
                                Text(text = stringResource(R.string.action_retry))
                            }
                        }
                    }
                }
                AlbumsUiState.Empty -> {
                    AlbumsEmptyState(
                        horizontalPadding = horizontalPadding,
                        isOfflineMode = isOfflineMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                is AlbumsUiState.Success -> {
                    val items = lazyPagingItems ?: return@Column
                    when {
                        !isOfflineMode && items.loadState.refresh is LoadState.Loading
                            && items.itemCount == 0 -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(spacing.medium))
                                    Text(
                                        text = stringResource(R.string.albums_loading),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        items.loadState.refresh is LoadState.Error -> {
                            val error = items.loadState.refresh as LoadState.Error
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = horizontalPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                                ) {
                                    ErrorCard(
                                        message = error.error.message
                                            ?: stringResource(R.string.albums_error)
                                    )
                                    Button(onClick = { items.retry() }) {
                                        Text(text = stringResource(R.string.action_retry))
                                    }
                                }
                            }
                        }
                        items.itemCount == 0 -> {
                            AlbumsEmptyState(
                                horizontalPadding = horizontalPadding,
                                isOfflineMode = isOfflineMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                        else -> {
                            if (isListView) {
                                AnimatedContent(
                                    targetState = isExpanded,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "albumsListLayout",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) { expanded ->
                                    if (expanded) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(spacing.large)
                                        ) {
                                            AlphabetFastScroller(
                                                modifier = Modifier
                                                    .weight(0.6f)
                                                    .fillMaxHeight(),
                                                isEnabled = showAlphabetScroller,
                                                onLetterChange = { letter ->
                                                    alphabetIndexMap[letter]?.let { index ->
                                                        scrollScope.launch {
                                                            expandedListState.scrollToItem(index)
                                                        }
                                                    }
                                                }
                                            ) {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    state = expandedListState,
                                                    contentPadding = listPadding
                                                ) {
                                                    items(
                                                        count = items.itemCount,
                                                        key = { index ->
                                                            items[index]?.let { album ->
                                                                "${album.provider}:${album.itemId}"
                                                            } ?: "placeholder_$index"
                                                        }
                                                    ) { index ->
                                                        val album = items[index]
                                                        if (album != null) {
                                                            AlbumListItem(
                                                                album = album,
                                                                onClick = { handleAlbumClick(album) },
                                                                showDivider = index < items.itemCount - 1,
                                                                isOfflineMode = isOfflineMode,
                                                                imageQualityManager = imageQualityManager
                                                            )
                                                        } else {
                                                            AlbumListItemPlaceholder(
                                                                showDivider = index < items.itemCount - 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            AlbumDetailPane(
                                                album = selectedAlbum,
                                                artworkSize = artworkSize,
                                                onOpenAlbum = onAlbumClick,
                                                isOfflineMode = isOfflineMode,
                                                imageQualityManager = imageQualityManager,
                                                modifier = Modifier
                                                    .weight(0.4f)
                                                    .fillMaxHeight()
                                                    .padding(
                                                        top = listVerticalPadding,
                                                        bottom = listVerticalPadding,
                                                        end = horizontalPadding
                                                    )
                                            )
                                        }
                                    } else {
                                        AlphabetFastScroller(
                                            modifier = Modifier.fillMaxSize(),
                                            isEnabled = showAlphabetScroller,
                                            onLetterChange = { letter ->
                                                alphabetIndexMap[letter]?.let { index ->
                                                    scrollScope.launch {
                                                        collapsedListState.scrollToItem(index)
                                                    }
                                                }
                                            }
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                state = collapsedListState,
                                                contentPadding = listPadding
                                            ) {
                                                items(
                                                    count = items.itemCount,
                                                    key = { index ->
                                                        items[index]?.let { album ->
                                                            "${album.provider}:${album.itemId}"
                                                        } ?: "placeholder_$index"
                                                    }
                                                ) { index ->
                                                    val album = items[index]
                                                    if (album != null) {
                                                        AlbumListItem(
                                                            album = album,
                                                            onClick = { handleAlbumClick(album) },
                                                            showDivider = index < items.itemCount - 1,
                                                            isOfflineMode = isOfflineMode,
                                                            imageQualityManager = imageQualityManager
                                                        )
                                                    } else {
                                                        AlbumListItemPlaceholder(
                                                            showDivider = index < items.itemCount - 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                AnimatedContent(
                                    targetState = isExpanded,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "albumsLayout",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) { expanded ->
                                    if (expanded) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(spacing.large)
                                        ) {
                                            AlphabetFastScroller(
                                                modifier = Modifier
                                                    .weight(0.6f)
                                                    .fillMaxHeight(),
                                                isEnabled = showAlphabetScroller,
                                                onLetterChange = { letter ->
                                                    alphabetIndexMap[letter]?.let { index ->
                                                        scrollScope.launch {
                                                            expandedGridState.scrollToItem(index)
                                                        }
                                                    }
                                                }
                                            ) {
                                                AlbumGrid(
                                                    albums = items,
                                                    onAlbumClick = handleAlbumClick,
                                                    gridState = expandedGridState,
                                                    columns = columns,
                                                    artworkSize = artworkSize,
                                                    contentPadding = gridPadding,
                                                    isOfflineMode = isOfflineMode,
                                                    imageQualityManager = imageQualityManager,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            AlbumDetailPane(
                                                album = selectedAlbum,
                                                artworkSize = artworkSize,
                                                onOpenAlbum = onAlbumClick,
                                                isOfflineMode = isOfflineMode,
                                                imageQualityManager = imageQualityManager,
                                                modifier = Modifier
                                                    .weight(0.4f)
                                                    .fillMaxHeight()
                                                    .padding(
                                                        top = gridVerticalPadding,
                                                        bottom = gridVerticalPadding,
                                                        end = horizontalPadding
                                                    )
                                            )
                                        }
                                    } else {
                                        AlphabetFastScroller(
                                            modifier = Modifier.fillMaxSize(),
                                            isEnabled = showAlphabetScroller,
                                            onLetterChange = { letter ->
                                                alphabetIndexMap[letter]?.let { index ->
                                                    scrollScope.launch {
                                                        collapsedGridState.scrollToItem(index)
                                                    }
                                                }
                                            }
                                        ) {
                                            AlbumGrid(
                                                albums = items,
                                                onAlbumClick = handleAlbumClick,
                                                gridState = collapsedGridState,
                                                columns = columns,
                                                artworkSize = artworkSize,
                                                contentPadding = gridPadding,
                                                isOfflineMode = isOfflineMode,
                                                imageQualityManager = imageQualityManager,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun AlbumDetailPane(
    album: Album?,
    artworkSize: Dp,
    onOpenAlbum: (Album) -> Unit,
    isOfflineMode: Boolean,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    if (album == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(spacing.extraLarge),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.albums_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val context = LocalContext.current
    val optimizedSize = imageQualityManager.getOptimalImageSize(artworkSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val bitmapConfig = imageQualityManager.getOptimalBitmapConfig()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        AsyncImage(
            model = buildAlbumArtworkRequest(
                context = context,
                album = album,
                sizePx = sizePx,
                bitmapConfig = bitmapConfig,
                isOfflineMode = isOfflineMode
            ),
            contentDescription = stringResource(R.string.content_desc_album_artwork),
            placeholder = placeholder,
            error = placeholder,
            modifier = Modifier
                .width(optimizedSize)
                .height(optimizedSize),
        )
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = album.artists.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = { onOpenAlbum(album) }) {
            Text(text = stringResource(R.string.action_open_album))
        }
    }
}

@Composable
private fun AlbumsEmptyState(
    horizontalPadding: Dp,
    isOfflineMode: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
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
                text = if (isOfflineMode) {
                    stringResource(R.string.no_downloaded_content)
                } else {
                    stringResource(R.string.albums_empty)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val AlbumSaver = Saver<Album?, String>(
    save = { album -> album?.let { Json.encodeToString(it) } },
    restore = { json -> json?.let { Json.decodeFromString<Album>(it) } }
)
