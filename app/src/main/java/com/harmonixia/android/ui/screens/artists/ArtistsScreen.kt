package com.harmonixia.android.ui.screens.artists

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
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
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.ui.components.ArtistListItem
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.OfflineModeBanner
import com.harmonixia.android.ui.navigation.MainScaffoldActions
import com.harmonixia.android.ui.screens.settings.SettingsTab
import com.harmonixia.android.ui.theme.rememberAdaptiveSpacing
import com.harmonixia.android.util.ImageQualityManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    onNavigateToSettings: (SettingsTab?) -> Unit,
    onArtistClick: (Artist) -> Unit,
    viewModel: ArtistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val albumCounts by viewModel.artistAlbumCounts.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()

    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    val configuration = LocalConfiguration.current
    val spacing = rememberAdaptiveSpacing()
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = spacing.large
    val listVerticalPadding = if (isLandscape) spacing.medium * 0.75f else spacing.medium
    val listPadding = PaddingValues(horizontal = horizontalPadding, vertical = listVerticalPadding)

    val successState = uiState as? ArtistsUiState.Success
    val lazyPagingItems = successState?.artists?.collectAsLazyPagingItems()
    val isRefreshing = lazyPagingItems?.loadState?.refresh is LoadState.Loading

    val titleText = if (uiState is ArtistsUiState.Success) {
        stringResource(R.string.artists_count, lazyPagingItems?.itemCount ?: 0)
    } else {
        stringResource(R.string.artists_title)
    }

    var selectedArtist by rememberSaveable(stateSaver = ArtistSaver) {
        mutableStateOf<Artist?>(null)
    }
    val selectedArtistKey = selectedArtist?.let { "${it.provider}:${it.itemId}" }
    val selectedAlbumCount = selectedArtistKey?.let { albumCounts[it] } ?: 0
    LaunchedEffect(selectedArtistKey) {
        selectedArtist?.let { viewModel.loadAlbumCount(it) }
    }
    val handleArtistClick: (Artist) -> Unit = { artist ->
        if (isExpanded) {
            selectedArtist = artist
        } else {
            onArtistClick(artist)
        }
    }

    val latestIsExpanded by rememberUpdatedState(isExpanded)
    val latestSelectedArtist by rememberUpdatedState(selectedArtist)
    val latestOnArtistClick by rememberUpdatedState(onArtistClick)
    DisposableEffect(windowSizeClass.widthSizeClass) {
        val wasExpanded = isExpanded
        onDispose {
            if (wasExpanded && !latestIsExpanded) {
                latestSelectedArtist?.let { latestOnArtistClick(it) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = titleText) },
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
            onRefresh = { lazyPagingItems?.refresh() ?: viewModel.refresh() },
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
                    ArtistsUiState.Loading -> {
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
                                    text = stringResource(R.string.artists_loading),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    is ArtistsUiState.Error -> {
                        val message = state.message.ifBlank {
                            stringResource(R.string.artists_error)
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
                    ArtistsUiState.Empty -> {
                        ArtistsEmptyState(
                            horizontalPadding = horizontalPadding,
                            isOfflineMode = isOfflineMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                    is ArtistsUiState.Success -> {
                        val items = lazyPagingItems ?: return@Column
                        when {
                            !isOfflineMode && items.loadState.refresh is LoadState.Loading -> {
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
                                            text = stringResource(R.string.artists_loading),
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
                                                ?: stringResource(R.string.artists_error)
                                        )
                                        Button(onClick = { items.retry() }) {
                                            Text(text = stringResource(R.string.action_retry))
                                        }
                                    }
                                }
                            }
                            items.itemCount == 0 -> {
                                ArtistsEmptyState(
                                    horizontalPadding = horizontalPadding,
                                    isOfflineMode = isOfflineMode,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                            }
                            else -> {
                                AnimatedContent(
                                    targetState = isExpanded,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "artistsLayout",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) { expanded ->
                                    if (expanded) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(spacing.large)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .weight(0.6f)
                                                    .fillMaxHeight(),
                                                contentPadding = listPadding
                                            ) {
                                                items(
                                                    count = items.itemCount,
                                                    key = { index ->
                                                        items[index]?.let { artist ->
                                                            "${artist.provider}:${artist.itemId}"
                                                        } ?: "placeholder_$index"
                                                    }
                                                ) { index ->
                                                    val artist = items[index]
                                                    if (artist != null) {
                                                        ArtistListItem(
                                                            artist = artist,
                                                            onClick = { handleArtistClick(artist) },
                                                            showDivider = index < items.itemCount - 1
                                                        )
                                                    } else {
                                                        ArtistListItemPlaceholder(
                                                            showDivider = index < items.itemCount - 1
                                                        )
                                                    }
                                                }
                                            }
                                            ArtistDetailPane(
                                                artist = selectedArtist,
                                                albumCount = selectedAlbumCount,
                                                onViewAlbums = onArtistClick,
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
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = listPadding
                                        ) {
                                            items(
                                                count = items.itemCount,
                                                key = { index ->
                                                    items[index]?.let { artist ->
                                                        "${artist.provider}:${artist.itemId}"
                                                    } ?: "placeholder_$index"
                                                }
                                            ) { index ->
                                                val artist = items[index]
                                                if (artist != null) {
                                                    ArtistListItem(
                                                        artist = artist,
                                                        onClick = { handleArtistClick(artist) },
                                                        showDivider = index < items.itemCount - 1
                                                    )
                                                } else {
                                                    ArtistListItemPlaceholder(
                                                        showDivider = index < items.itemCount - 1
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
}

@Composable
private fun ArtistListItemPlaceholder(
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                )
            },
            headlineContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        )
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
        }
    }
}

@Composable
private fun ArtistsEmptyState(
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
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isOfflineMode) {
                    stringResource(R.string.no_downloaded_content)
                } else {
                    stringResource(R.string.artists_empty)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ArtistDetailPane(
    artist: Artist?,
    albumCount: Int,
    onViewAlbums: (Artist) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    if (artist == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(spacing.extraLarge),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.artist_detail_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val context = LocalContext.current
    val qualityManager = remember(context) { ImageQualityManager(context) }
    val avatarSize = 120.dp
    val optimizedSize = qualityManager.getOptimalImageSize(avatarSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val displayName = artist.name.ifBlank { artist.sortName.orEmpty() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (artist.imageUrl.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(optimizedSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = stringResource(R.string.content_desc_artist_image),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val imageRequest = ImageRequest.Builder(context)
                .data(artist.imageUrl)
                .size(sizePx)
                .bitmapConfig(qualityManager.getOptimalBitmapConfig())
                .build()
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.content_desc_artist_image),
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(optimizedSize)
                    .clip(CircleShape)
            )
        }
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.artist_detail_album_count, albumCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = { onViewAlbums(artist) }) {
            Text(text = stringResource(R.string.action_view_albums))
        }
    }
}

private val ArtistSaver = Saver<Artist?, String>(
    save = { artist -> artist?.let { Json.encodeToString(it) } },
    restore = { json -> json?.let { Json.decodeFromString<Artist>(it) } }
)
