package com.harmonixia.android.ui.screens.albums

import android.app.Activity
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil3.compose.AsyncImage
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.OfflineModeBanner
import com.harmonixia.android.ui.components.PlaylistPickerDialog
import com.harmonixia.android.ui.components.TrackList
import com.harmonixia.android.ui.screens.settings.SettingsTab
import com.harmonixia.android.ui.screens.playlists.CreatePlaylistDialog
import com.harmonixia.android.ui.theme.rememberAdaptiveSpacing
import com.harmonixia.android.ui.util.buildAlbumArtworkRequest
import com.harmonixia.android.util.ImageQualityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: (SettingsTab?) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val album by viewModel.album.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val imageQualityManager = viewModel.imageQualityManager
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showPlaylistPicker by remember { mutableStateOf(false) }
    var pendingTrack by remember { mutableStateOf<Track?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AlbumDetailUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }
                AlbumDetailUiEvent.PlaylistCreated -> {
                    showCreateDialog = false
                    playlistName = ""
                    if (pendingTrack != null) {
                        showPlaylistPicker = true
                    }
                }
            }
        }
    }

    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    val configuration = LocalConfiguration.current
    val spacing = rememberAdaptiveSpacing()
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = spacing.large
    val baseArtworkSize by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 300.dp
                WindowWidthSizeClass.Medium -> 340.dp
                WindowWidthSizeClass.Expanded -> 420.dp
                else -> 300.dp
            }
        }
    }
    val artworkSize = if (isLandscape) baseArtworkSize * 0.8f else baseArtworkSize
    val useWideLayout by remember(windowSizeClass, configuration) {
        derivedStateOf {
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                configuration.screenWidthDp >= 800
        }
    }
    val isVeryWide = configuration.screenWidthDp > 1200
    val sectionHeaderStyle = if (isExpanded) {
        MaterialTheme.typography.headlineSmall
    } else {
        MaterialTheme.typography.titleLarge
    }
    val albumTitleStyle = if (isExpanded) {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.headlineSmall
    }
    val artistNameStyle = if (isExpanded) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val trackTitleStyle: TextStyle? = if (isExpanded) {
        MaterialTheme.typography.bodyLarge
    } else {
        null
    }
    val trackMetaStyle: TextStyle? = if (isExpanded) {
        MaterialTheme.typography.bodyMedium
    } else {
        null
    }
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val appBarArtworkSize = 24.dp
    val appBarRevealThresholdPx = with(density) {
        (artworkSize + spacing.large).toPx()
    }
    val showAppBarThumbnail by remember(listState, isVeryWide, appBarRevealThresholdPx) {
        derivedStateOf {
            !isVeryWide &&
                (listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > appBarRevealThresholdPx)
        }
    }

    val titleText = album?.name?.ifBlank {
        stringResource(R.string.album_detail_title)
    } ?: stringResource(R.string.album_detail_title)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showAppBarThumbnail) {
                                AlbumArtwork(
                                    album = album,
                                    displaySize = appBarArtworkSize,
                                    requestSize = appBarArtworkSize,
                                    cornerRadius = 8.dp,
                                    useOptimizedDisplaySize = false,
                                    isOfflineMode = isOfflineMode,
                                    imageQualityManager = imageQualityManager,
                                )
                                Spacer(modifier = Modifier.width(spacing.small))
                            }
                            Text(
                                text = titleText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigateToSettings(null) }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.action_open_settings)
                            )
                        }
                    }
                )
                if (isOfflineMode) {
                    OfflineModeBanner(
                        text = stringResource(R.string.offline_mode_active),
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding, vertical = spacing.small)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = uiState) {
            AlbumDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(spacing.medium))
                        Text(
                            text = stringResource(R.string.album_detail_loading_tracks),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            is AlbumDetailUiState.Error -> {
                val message = state.message.ifBlank {
                    stringResource(R.string.albums_error)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = horizontalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        ErrorCard(message = message)
                        Button(onClick = viewModel::loadAlbumTracks) {
                            Text(text = stringResource(R.string.action_retry))
                        }
                    }
                }
            }
            AlbumDetailUiState.Empty,
            AlbumDetailUiState.Metadata,
            is AlbumDetailUiState.Cached,
            is AlbumDetailUiState.Success -> {
                val resolvedAlbum = when (state) {
                    is AlbumDetailUiState.Cached -> state.album
                    is AlbumDetailUiState.Success -> state.album
                    else -> album
                }
                val tracks = when (state) {
                    is AlbumDetailUiState.Cached -> state.tracks
                    is AlbumDetailUiState.Success -> state.tracks
                    else -> emptyList()
                }
                val isInitialLoading = state is AlbumDetailUiState.Metadata
                val isRefreshing =
                    (state as? AlbumDetailUiState.Cached)?.isRefreshing == true || isInitialLoading
                val hasMore = (state as? AlbumDetailUiState.Success)?.hasMore == true
                val isLoadingMore = (state as? AlbumDetailUiState.Success)?.isLoadingMore == true
                val indexById = remember(tracks) {
                    tracks.withIndex().associate { indexed -> indexed.value.itemId to indexed.index }
                }
                val indexProvider: (Track, Int) -> Int = { track, _ ->
                    indexById[track.itemId] ?: 0
                }
                val leftTracks = remember(tracks) {
                    tracks.filterIndexed { index, _ -> index % 2 == 0 }
                }
                val rightTracks = remember(tracks) {
                    tracks.filterIndexed { index, _ -> index % 2 == 1 }
                }
                if (isVeryWide) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                            AlbumHeader(
                                album = resolvedAlbum,
                                artworkSize = artworkSize,
                                useWideLayout = useWideLayout,
                                canPlay = tracks.isNotEmpty(),
                                onPlayAlbum = { viewModel.playAlbumSequential() },
                                onShuffleAlbum = { viewModel.shuffleAlbum() },
                                isOfflineMode = isOfflineMode,
                                titleStyle = albumTitleStyle,
                                artistStyle = artistNameStyle,
                                rowSpacing = if (isExpanded) 32.dp else 24.dp,
                                imageQualityManager = imageQualityManager
                            )
                            Spacer(modifier = Modifier.height(spacing.extraLarge))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.small)
                            ) {
                                Text(
                                    text = stringResource(R.string.album_detail_tracks),
                                    style = sectionHeaderStyle
                                )
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(spacing.medium))
                        }
                        if (tracks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = horizontalPadding, vertical = spacing.large),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isInitialLoading) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(spacing.small))
                                        Text(
                                            text = stringResource(R.string.album_detail_loading_tracks),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (isOfflineMode) {
                                            stringResource(R.string.no_downloaded_content)
                                        } else {
                                            stringResource(R.string.album_detail_no_tracks)
                                        },
                                        style = sectionHeaderStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = horizontalPadding),
                                horizontalArrangement = Arrangement.spacedBy(spacing.large)
                            ) {
                                TrackList(
                                    tracks = leftTracks,
                                    onTrackClick = viewModel::playTrack,
                                    showContextMenu = true,
                                    isEditable = false,
                                    onAddToPlaylist = { track ->
                                        pendingTrack = track
                                        viewModel.refreshPlaylists()
                                        showPlaylistPicker = true
                                    },
                                    onAddToFavorites = { track -> viewModel.addTrackToFavorites(track) },
                                    onRemoveFromFavorites = {
                                        track -> viewModel.removeTrackFromFavorites(track)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentPadding = PaddingValues(vertical = spacing.large),
                                    trackTitleTextStyle = trackTitleStyle,
                                    trackSupportingTextStyle = trackMetaStyle,
                                    trackMetadataTextStyle = trackMetaStyle,
                                    indexProvider = indexProvider,
                                    hasMore = hasMore,
                                    isLoadingMore = isLoadingMore,
                                    onLoadMore = viewModel::loadMoreTracks,
                                    showEmptyState = false
                                )
                                TrackList(
                                    tracks = rightTracks,
                                    onTrackClick = viewModel::playTrack,
                                    showContextMenu = true,
                                    isEditable = false,
                                    onAddToPlaylist = { track ->
                                        pendingTrack = track
                                        viewModel.refreshPlaylists()
                                        showPlaylistPicker = true
                                    },
                                    onAddToFavorites = { track -> viewModel.addTrackToFavorites(track) },
                                    onRemoveFromFavorites = {
                                        track -> viewModel.removeTrackFromFavorites(track)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentPadding = PaddingValues(vertical = spacing.large),
                                    trackTitleTextStyle = trackTitleStyle,
                                    trackSupportingTextStyle = trackMetaStyle,
                                    trackMetadataTextStyle = trackMetaStyle,
                                    indexProvider = indexProvider,
                                    hasMore = hasMore,
                                    isLoadingMore = isLoadingMore,
                                    onLoadMore = viewModel::loadMoreTracks,
                                    showEmptyState = false
                                )
                            }
                        }
                    }
                } else {
                    val contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = spacing.large,
                        bottom = spacing.large
                    )

                    TrackList(
                        tracks = tracks,
                        onTrackClick = viewModel::playTrack,
                        showContextMenu = true,
                        isEditable = false,
                        onAddToPlaylist = { track ->
                            pendingTrack = track
                            viewModel.refreshPlaylists()
                            showPlaylistPicker = true
                        },
                        onAddToFavorites = { track -> viewModel.addTrackToFavorites(track) },
                        onRemoveFromFavorites = {
                            track -> viewModel.removeTrackFromFavorites(track)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        listState = listState,
                        contentPadding = contentPadding,
                        headerContent = {
                            item {
                                AlbumDetails(
                                    album = resolvedAlbum,
                                    titleStyle = albumTitleStyle,
                                    artistStyle = artistNameStyle,
                                    canPlay = tracks.isNotEmpty(),
                                    onPlayAlbum = { viewModel.playAlbumSequential() },
                                    onShuffleAlbum = { viewModel.shuffleAlbum() },
                                    isOfflineMode = isOfflineMode,
                                    artworkSize = artworkSize,
                                    imageQualityManager = imageQualityManager
                                )
                            }
                            item { Spacer(modifier = Modifier.height(spacing.extraLarge)) }
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                                ) {
                                    Text(
                                        text = stringResource(R.string.album_detail_tracks),
                                        style = sectionHeaderStyle
                                    )
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(spacing.medium)) }
                            if (isInitialLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = spacing.large),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.height(spacing.small))
                                            Text(
                                                text = stringResource(
                                                    R.string.album_detail_loading_tracks
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        trackTitleTextStyle = trackTitleStyle,
                        trackSupportingTextStyle = trackMetaStyle,
                        trackMetadataTextStyle = trackMetaStyle,
                        indexProvider = indexProvider,
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        onLoadMore = viewModel::loadMoreTracks,
                        showEmptyState = !isInitialLoading
                    )
                }
            }
        }
    }

    if (showPlaylistPicker) {
        val track = pendingTrack
        if (track != null) {
            PlaylistPickerDialog(
                playlists = playlists,
                onPlaylistSelected = { playlist ->
                    viewModel.addTrackToPlaylist(playlist, track)
                    showPlaylistPicker = false
                    pendingTrack = null
                },
                onDismiss = { showPlaylistPicker = false },
                onCreateNew = {
                    showPlaylistPicker = false
                    showCreateDialog = true
                }
            )
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            name = playlistName,
            onNameChange = { playlistName = it },
            onConfirm = { viewModel.createPlaylist(playlistName) },
            onDismiss = {
                showCreateDialog = false
                playlistName = ""
            }
        )
    }
}

@Composable
private fun AlbumArtwork(
    album: Album?,
    displaySize: Dp,
    requestSize: Dp = displaySize,
    cornerRadius: Dp,
    useOptimizedDisplaySize: Boolean = requestSize == displaySize,
    isOfflineMode: Boolean,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val context = LocalContext.current
    val optimizedRequestSize = imageQualityManager.getOptimalImageSize(requestSize)
    val optimizedDisplaySize = if (useOptimizedDisplaySize) {
        optimizedRequestSize
    } else {
        displaySize
    }
    val sizePx = with(LocalDensity.current) { optimizedRequestSize.roundToPx() }
    val bitmapConfig = imageQualityManager.getOptimalBitmapConfig()
    val imageRequest = buildAlbumArtworkRequest(
        context = context,
        album = album,
        sizePx = sizePx,
        bitmapConfig = bitmapConfig,
        isOfflineMode = isOfflineMode
    )

    AsyncImage(
        model = imageRequest,
        contentDescription = stringResource(R.string.content_desc_album_artwork),
        placeholder = placeholder,
        error = placeholder,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(optimizedDisplaySize)
            .clip(RoundedCornerShape(cornerRadius))
    )
}

@Composable
private fun AlbumDetails(
    album: Album?,
    titleStyle: TextStyle,
    artistStyle: TextStyle,
    canPlay: Boolean,
    onPlayAlbum: () -> Unit,
    onShuffleAlbum: () -> Unit,
    isOfflineMode: Boolean,
    artworkSize: Dp,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    val artistNames = album?.artists?.joinToString(", ").orEmpty()
    val albumTitle = album?.name?.ifBlank {
        stringResource(R.string.album_detail_title)
    } ?: stringResource(R.string.album_detail_title)
    val buttonRowModifier = Modifier.fillMaxWidth(0.8f)
    val canPlayTracks = album != null && canPlay
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        AlbumArtwork(
            album = album,
            displaySize = artworkSize,
            requestSize = artworkSize,
            cornerRadius = 20.dp,
            isOfflineMode = isOfflineMode,
            imageQualityManager = imageQualityManager
        )
        Text(
            text = albumTitle,
            style = titleStyle,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (artistNames.isNotBlank()) {
            Text(
                text = artistNames,
                style = artistStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Row(
                modifier = buttonRowModifier,
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                val buttonModifier = Modifier.weight(1f)
                FilledTonalButton(
                    onClick = onPlayAlbum,
                    enabled = canPlayTracks,
                    modifier = buttonModifier
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.action_play)
                    )
                }
                FilledTonalButton(
                    onClick = onShuffleAlbum,
                    enabled = canPlayTracks,
                    modifier = buttonModifier
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = stringResource(R.string.action_shuffle)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(
    album: Album?,
    artworkSize: Dp,
    useWideLayout: Boolean,
    canPlay: Boolean,
    onPlayAlbum: () -> Unit,
    onShuffleAlbum: () -> Unit,
    isOfflineMode: Boolean,
    titleStyle: TextStyle,
    artistStyle: TextStyle,
    rowSpacing: Dp,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    val artistNames = album?.artists?.joinToString(", ").orEmpty()
    val albumTitle = album?.name?.ifBlank {
        stringResource(R.string.album_detail_title)
    } ?: stringResource(R.string.album_detail_title)
    val buttonRowModifier = if (useWideLayout) Modifier else Modifier.fillMaxWidth(0.8f)
    val canPlayTracks = album != null && canPlay

    if (useWideLayout) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(rowSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtwork(
                album = album,
                displaySize = artworkSize,
                requestSize = artworkSize,
                cornerRadius = 20.dp,
                isOfflineMode = isOfflineMode,
                imageQualityManager = imageQualityManager
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Text(
                    text = albumTitle,
                    style = titleStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (artistNames.isNotBlank()) {
                    Text(
                        text = artistNames,
                        style = artistStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Row(
                        modifier = buttonRowModifier,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        val buttonModifier = if (useWideLayout) Modifier else Modifier.weight(1f)
                        FilledTonalButton(
                            onClick = onPlayAlbum,
                            enabled = canPlayTracks,
                            modifier = buttonModifier
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.action_play)
                            )
                        }
                        FilledTonalButton(
                            onClick = onShuffleAlbum,
                            enabled = canPlayTracks,
                            modifier = buttonModifier
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = stringResource(R.string.action_shuffle)
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            AlbumArtwork(
                album = album,
                displaySize = artworkSize,
                requestSize = artworkSize,
                cornerRadius = 20.dp,
                isOfflineMode = isOfflineMode,
                imageQualityManager = imageQualityManager
            )
            Text(
                text = albumTitle,
                style = titleStyle,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (artistNames.isNotBlank()) {
                Text(
                    text = artistNames,
                    style = artistStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Row(
                    modifier = buttonRowModifier,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    val buttonModifier = if (useWideLayout) Modifier else Modifier.weight(1f)
                    FilledTonalButton(
                        onClick = onPlayAlbum,
                        enabled = canPlayTracks,
                        modifier = buttonModifier
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.action_play)
                        )
                    }
                    FilledTonalButton(
                        onClick = onShuffleAlbum,
                        enabled = canPlayTracks,
                        modifier = buttonModifier
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = stringResource(R.string.action_shuffle)
                        )
                    }
                }
            }
        }
    }
}
