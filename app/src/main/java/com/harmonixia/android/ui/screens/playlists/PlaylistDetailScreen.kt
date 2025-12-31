package com.harmonixia.android.ui.screens.playlists

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.OfflineModeBanner
import com.harmonixia.android.ui.components.PlaylistOptionsMenu
import com.harmonixia.android.ui.components.PlaylistPickerDialog
import com.harmonixia.android.ui.components.RenamePlaylistDialog
import com.harmonixia.android.ui.components.TrackList
import com.harmonixia.android.ui.screens.settings.SettingsTab
import com.harmonixia.android.ui.theme.rememberAdaptiveSpacing
import com.harmonixia.android.ui.util.PlaylistCoverEntryPoint
import com.harmonixia.android.ui.util.PlaylistCoverGenerator
import com.harmonixia.android.util.ImageQualityManager
import dagger.hilt.android.EntryPointAccessors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: (SettingsTab?) -> Unit,
    onNavigateToPlaylist: (Playlist) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isRenaming by viewModel.isRenaming.collectAsStateWithLifecycle()
    val renameErrorMessageResId by viewModel.renameErrorMessageResId.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val isFavoritesPlaylist = playlist?.itemId == "favorites" && playlist?.provider == "harmonixia"
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showPlaylistPicker by remember { mutableStateOf(false) }
    var pendingTrack by remember { mutableStateOf<Track?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf("") }
    var playlistName by remember { mutableStateOf("") }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PlaylistDetailUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }
                PlaylistDetailUiEvent.PlaylistCreated -> {
                    showCreateDialog = false
                    playlistName = ""
                    if (pendingTrack != null) {
                        showPlaylistPicker = true
                    }
                }
                PlaylistDetailUiEvent.PlaylistDeleted -> onNavigateBack()
                is PlaylistDetailUiEvent.PlaylistRenamed -> {
                    showRenameDialog = false
                    renameValue = ""
                    onNavigateToPlaylist(event.playlist)
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
                WindowWidthSizeClass.Compact -> 260.dp
                WindowWidthSizeClass.Medium -> 300.dp
                WindowWidthSizeClass.Expanded -> 380.dp
                else -> 260.dp
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
    val playlistTitleStyle = if (isExpanded) {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.headlineSmall
    }
    val ownerStyle = if (isExpanded) {
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
    val minArtworkSize = 32.dp
    val collapseRangePx = with(density) {
        (artworkSize - minArtworkSize + spacing.large).coerceAtLeast(0.dp).toPx()
    }
    var headerOffsetPx by remember { mutableStateOf(0f) }
    LaunchedEffect(collapseRangePx) {
        headerOffsetPx = headerOffsetPx.coerceIn(0f, collapseRangePx)
    }
    val isListAtTop by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
        }
    }
    val nestedScrollConnection = remember(collapseRangePx, listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (collapseRangePx == 0f) return Offset.Zero
                val delta = available.y
                return when {
                    delta < 0f && headerOffsetPx < collapseRangePx -> {
                        val newOffset = (headerOffsetPx - delta).coerceIn(0f, collapseRangePx)
                        val consumed = newOffset - headerOffsetPx
                        headerOffsetPx = newOffset
                        Offset(0f, -consumed)
                    }
                    delta > 0f && headerOffsetPx > 0f && isListAtTop -> {
                        val newOffset = (headerOffsetPx - delta).coerceIn(0f, collapseRangePx)
                        val consumed = headerOffsetPx - newOffset
                        headerOffsetPx = newOffset
                        Offset(0f, consumed)
                    }
                    else -> Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (collapseRangePx == 0f) return Offset.Zero
                val delta = available.y
                if (delta > 0f && headerOffsetPx > 0f && isListAtTop) {
                    val newOffset = (headerOffsetPx - delta).coerceIn(0f, collapseRangePx)
                    val consumedByHeader = headerOffsetPx - newOffset
                    headerOffsetPx = newOffset
                    return Offset(0f, consumedByHeader)
                }
                return Offset.Zero
            }
        }
    }
    val collapseFraction = if (collapseRangePx == 0f) {
        1f
    } else {
        (headerOffsetPx / collapseRangePx).coerceIn(0f, 1f)
    }
    val currentArtworkSize = (
        artworkSize.value +
            (minArtworkSize.value - artworkSize.value) * collapseFraction
    ).dp
    val artworkTopPadding = (spacing.large.value * (1f - collapseFraction)).dp
    val desiredTop = (artworkTopPadding + currentArtworkSize + spacing.medium)
        .coerceAtLeast(0.dp)
    val appBarThumbnailAlpha by remember(collapseFraction, isVeryWide) {
        derivedStateOf {
            if (isVeryWide) 0f else ((collapseFraction - 0.2f) / 0.8f).coerceIn(0f, 1f)
        }
    }

    val titleText = playlist?.name?.ifBlank {
        stringResource(R.string.playlist_detail_title)
    } ?: stringResource(R.string.playlist_detail_title)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (appBarThumbnailAlpha > 0f) {
                                PlaylistArtwork(
                                    playlist = playlist,
                                    displaySize = minArtworkSize,
                                    requestSize = minArtworkSize,
                                    cornerRadius = 8.dp,
                                    useOptimizedDisplaySize = false,
                                    modifier = Modifier.alpha(appBarThumbnailAlpha)
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
                        if (playlist?.isEditable == true && !isFavoritesPlaylist) {
                            Box {
                                IconButton(onClick = { showOptionsMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = stringResource(R.string.playlist_options_menu)
                                    )
                                }
                                PlaylistOptionsMenu(
                                    expanded = showOptionsMenu,
                                    onDismissRequest = { showOptionsMenu = false },
                                    onRename = {
                                        showOptionsMenu = false
                                        renameValue = playlist?.name.orEmpty()
                                        viewModel.clearRenameError()
                                        showRenameDialog = true
                                    },
                                    onDelete = {
                                        showOptionsMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
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
            PlaylistDetailUiState.Loading -> {
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
                            text = stringResource(R.string.playlist_detail_loading_tracks),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            is PlaylistDetailUiState.Error -> {
                val message = state.message.ifBlank {
                    stringResource(R.string.playlists_error)
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
                        Button(onClick = viewModel::loadPlaylistTracks) {
                            Text(text = stringResource(R.string.action_retry))
                        }
                    }
                }
            }
            PlaylistDetailUiState.Empty -> {
                PlaylistDetailEmptyContent(
                    playlist = playlist,
                    isOfflineMode = isOfflineMode,
                    artworkSize = artworkSize,
                    useWideLayout = useWideLayout,
                    horizontalPadding = horizontalPadding,
                    titleStyle = playlistTitleStyle,
                    ownerStyle = ownerStyle,
                    emptyTitleStyle = sectionHeaderStyle,
                    emptyBodyStyle = if (isExpanded) {
                        MaterialTheme.typography.titleSmall
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    rowSpacing = if (isExpanded) 32.dp else 24.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onPlayPlaylist = { viewModel.playPlaylist() }
                )
            }
            is PlaylistDetailUiState.Success -> {
                PlaylistDetailContent(
                    playlist = playlist,
                    tracks = state.tracks,
                    artworkSize = artworkSize,
                    currentArtworkSize = currentArtworkSize,
                    artworkTopPadding = artworkTopPadding,
                    collapseFraction = collapseFraction,
                    contentTopPadding = desiredTop,
                    useWideLayout = useWideLayout,
                    horizontalPadding = horizontalPadding,
                    isVeryWide = isVeryWide,
                    titleStyle = playlistTitleStyle,
                    ownerStyle = ownerStyle,
                    sectionHeaderStyle = sectionHeaderStyle,
                    trackTitleStyle = trackTitleStyle,
                    trackMetaStyle = trackMetaStyle,
                    rowSpacing = if (isExpanded) 32.dp else 24.dp,
                    listState = listState,
                    appBarThumbnailAlpha = appBarThumbnailAlpha,
                    nestedScrollConnection = nestedScrollConnection,
                    onPlayPlaylist = { viewModel.playPlaylist() },
                    onTrackClick = viewModel::playTrack,
                    onAddToPlaylist = { track ->
                        pendingTrack = track
                        viewModel.refreshPlaylists()
                        showPlaylistPicker = true
                    },
                    onAddToFavorites = viewModel::addTrackToFavorites,
                    onRemoveFromFavorites = viewModel::removeTrackFromFavorites,
                    onRemoveFromPlaylist = viewModel::removeTrackFromPlaylist,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }

    if (showPlaylistPicker) {
        val track = pendingTrack
        if (track != null) {
            PlaylistPickerDialog(
                playlists = playlists,
                onPlaylistSelected = { selected ->
                    viewModel.addTrackToPlaylist(
                        targetPlaylistId = selected.itemId,
                        isEditable = selected.isEditable,
                        track = track
                    )
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

    if (showRenameDialog) {
        val errorMessage = renameErrorMessageResId?.let { stringResource(it) }
        RenamePlaylistDialog(
            currentName = playlist?.name.orEmpty(),
            name = renameValue,
            onNameChange = {
                renameValue = it
                viewModel.clearRenameError()
            },
            onConfirm = { viewModel.renamePlaylist(renameValue) },
            onDismiss = {
                showRenameDialog = false
                renameValue = ""
                viewModel.clearRenameError()
            },
            isLoading = isRenaming,
            errorMessage = errorMessage
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(R.string.playlist_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.playlist_delete_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePlaylist()
                    }
                ) {
                    Text(text = stringResource(R.string.playlist_action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(R.string.action_back))
                }
            }
        )
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
private fun PlaylistDetailContent(
    playlist: Playlist?,
    tracks: List<Track>,
    artworkSize: Dp,
    currentArtworkSize: Dp,
    artworkTopPadding: Dp,
    collapseFraction: Float,
    contentTopPadding: Dp,
    useWideLayout: Boolean,
    horizontalPadding: Dp,
    isVeryWide: Boolean,
    titleStyle: TextStyle,
    ownerStyle: TextStyle,
    sectionHeaderStyle: TextStyle,
    trackTitleStyle: TextStyle?,
    trackMetaStyle: TextStyle?,
    rowSpacing: Dp,
    listState: LazyListState,
    appBarThumbnailAlpha: Float,
    nestedScrollConnection: NestedScrollConnection,
    onPlayPlaylist: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onAddToFavorites: (Track) -> Unit,
    onRemoveFromFavorites: (Track) -> Unit,
    onRemoveFromPlaylist: (Track, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
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
        Column(modifier = modifier) {
            Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                PlaylistHeader(
                    playlist = playlist,
                    artworkSize = artworkSize,
                    useWideLayout = useWideLayout,
                    canPlay = tracks.isNotEmpty(),
                    onPlayPlaylist = onPlayPlaylist,
                    titleStyle = titleStyle,
                    ownerStyle = ownerStyle,
                    rowSpacing = rowSpacing
                )
                Spacer(modifier = Modifier.height(spacing.extraLarge))
                Text(
                    text = stringResource(R.string.playlist_detail_tracks),
                    style = sectionHeaderStyle
                )
                Spacer(modifier = Modifier.height(spacing.medium))
            }
            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = spacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.playlist_detail_no_tracks),
                        style = sectionHeaderStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
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
                        onTrackClick = onTrackClick,
                        showContextMenu = true,
                        isEditable = playlist?.isEditable == true,
                        onAddToPlaylist = onAddToPlaylist,
                        onRemoveFromPlaylist = onRemoveFromPlaylist,
                        onAddToFavorites = onAddToFavorites,
                        onRemoveFromFavorites = onRemoveFromFavorites,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(vertical = spacing.large),
                        trackTitleTextStyle = trackTitleStyle,
                        trackSupportingTextStyle = trackMetaStyle,
                        trackMetadataTextStyle = trackMetaStyle,
                        indexProvider = indexProvider,
                        showEmptyState = false
                    )
                    TrackList(
                        tracks = rightTracks,
                        onTrackClick = onTrackClick,
                        showContextMenu = true,
                        isEditable = playlist?.isEditable == true,
                        onAddToPlaylist = onAddToPlaylist,
                        onRemoveFromPlaylist = onRemoveFromPlaylist,
                        onAddToFavorites = onAddToFavorites,
                        onRemoveFromFavorites = onRemoveFromFavorites,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(vertical = spacing.large),
                        trackTitleTextStyle = trackTitleStyle,
                        trackSupportingTextStyle = trackMetaStyle,
                        trackMetadataTextStyle = trackMetaStyle,
                        indexProvider = indexProvider,
                        showEmptyState = false
                    )
                }
            }
        }
    } else {
        val collapsedCornerRadius = 8.dp
        val pinnedCornerRadius = (
            20.dp.value +
                (collapsedCornerRadius.value - 20.dp.value) * collapseFraction
        ).dp
        val pinnedArtworkAlpha = (1f - appBarThumbnailAlpha).coerceIn(0f, 1f)
        val contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 0.dp,
            bottom = spacing.large
        )

        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentTopPadding)
                    .clipToBounds()
            ) {
                if (pinnedArtworkAlpha > 0f) {
                    PlaylistArtwork(
                        playlist = playlist,
                        displaySize = currentArtworkSize,
                        requestSize = artworkSize,
                        cornerRadius = pinnedCornerRadius,
                        useOptimizedDisplaySize = false,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = artworkTopPadding)
                            .alpha(pinnedArtworkAlpha)
                    )
                }
            }
            TrackList(
                tracks = tracks,
                onTrackClick = onTrackClick,
                showContextMenu = true,
                isEditable = playlist?.isEditable == true,
                onAddToPlaylist = onAddToPlaylist,
                onRemoveFromPlaylist = onRemoveFromPlaylist,
                onAddToFavorites = onAddToFavorites,
                onRemoveFromFavorites = onRemoveFromFavorites,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .nestedScroll(nestedScrollConnection),
                listState = listState,
                contentPadding = contentPadding,
                headerContent = {
                    item {
                        PlaylistDetails(
                            playlist = playlist,
                            canPlay = tracks.isNotEmpty(),
                            titleStyle = titleStyle,
                            ownerStyle = ownerStyle,
                            onPlayPlaylist = onPlayPlaylist
                        )
                    }
                    item { Spacer(modifier = Modifier.height(spacing.extraLarge)) }
                    item {
                        Text(
                            text = stringResource(R.string.playlist_detail_tracks),
                            style = sectionHeaderStyle
                        )
                    }
                    item { Spacer(modifier = Modifier.height(spacing.medium)) }
                },
                trackTitleTextStyle = trackTitleStyle,
                trackSupportingTextStyle = trackMetaStyle,
                trackMetadataTextStyle = trackMetaStyle,
                indexProvider = indexProvider
            )
        }
    }
}

@Composable
private fun PlaylistArtwork(
    playlist: Playlist?,
    displaySize: Dp,
    requestSize: Dp = displaySize,
    cornerRadius: Dp,
    useOptimizedDisplaySize: Boolean = requestSize == displaySize,
    modifier: Modifier = Modifier
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val context = LocalContext.current
    val qualityManager = remember(context) { ImageQualityManager(context) }
    val optimizedRequestSize = qualityManager.getOptimalImageSize(requestSize)
    val optimizedDisplaySize = if (useOptimizedDisplaySize) {
        optimizedRequestSize
    } else {
        displaySize
    }
    val sizePx = with(LocalDensity.current) { optimizedRequestSize.roundToPx() }
    val needsGeneratedCover = playlist?.imageUrl.isNullOrBlank()
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, PlaylistCoverEntryPoint::class.java)
    }
    val generator = remember(context) {
        PlaylistCoverGenerator(context, entryPoint.repository(), entryPoint.imageLoader())
    }
    val coverPath by produceState<String?>(
        initialValue = null,
        playlist?.itemId,
        playlist?.provider,
        sizePx,
        needsGeneratedCover
    ) {
        value = if (playlist != null && needsGeneratedCover) {
            generator.getCoverPath(playlist, sizePx)
        } else {
            null
        }
    }
    val imageData = coverPath?.let { File(it) } ?: playlist?.imageUrl
    val imageRequest = ImageRequest.Builder(context)
        .data(imageData)
        .size(sizePx)
        .bitmapConfig(qualityManager.getOptimalBitmapConfig())
        .build()

    AsyncImage(
        model = imageRequest,
        contentDescription = stringResource(R.string.content_desc_playlist_artwork),
        placeholder = placeholder,
        error = placeholder,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(optimizedDisplaySize)
            .clip(RoundedCornerShape(cornerRadius))
    )
}

@Composable
private fun PlaylistDetails(
    playlist: Playlist?,
    canPlay: Boolean,
    titleStyle: TextStyle,
    ownerStyle: TextStyle,
    onPlayPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    val playlistTitle = playlist?.name?.ifBlank {
        stringResource(R.string.playlist_detail_title)
    } ?: stringResource(R.string.playlist_detail_title)
    val owner = playlist?.owner?.trim().orEmpty()
    val buttonModifier = Modifier.fillMaxWidth(0.8f)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Text(
            text = playlistTitle,
            style = titleStyle,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (owner.isNotBlank()) {
            Text(
                text = stringResource(R.string.playlist_detail_owner, owner),
                style = ownerStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (playlist?.isEditable == true) {
            Text(
                text = stringResource(R.string.playlist_editable_badge),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            FilledTonalButton(
                onClick = onPlayPlaylist,
                enabled = playlist != null && canPlay,
                modifier = buttonModifier
            ) {
                Text(text = stringResource(R.string.playlist_detail_play))
            }
        }
    }
}

@Composable
private fun PlaylistDetailEmptyContent(
    playlist: Playlist?,
    isOfflineMode: Boolean,
    artworkSize: Dp,
    useWideLayout: Boolean,
    horizontalPadding: Dp,
    titleStyle: TextStyle,
    ownerStyle: TextStyle,
    emptyTitleStyle: TextStyle,
    emptyBodyStyle: TextStyle,
    rowSpacing: Dp,
    onPlayPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlaylistHeader(
            playlist = playlist,
            artworkSize = artworkSize,
            useWideLayout = useWideLayout,
            canPlay = false,
            onPlayPlaylist = onPlayPlaylist,
            titleStyle = titleStyle,
            ownerStyle = ownerStyle,
            rowSpacing = rowSpacing
        )
        Spacer(modifier = Modifier.height(spacing.extraLarge))
        Text(
            text = if (isOfflineMode) {
                stringResource(R.string.no_downloaded_content)
            } else {
                stringResource(R.string.playlist_detail_no_tracks)
            },
            style = emptyTitleStyle,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        Text(
            text = if (isOfflineMode) {
                stringResource(R.string.offline_mode_active)
            } else {
                stringResource(R.string.playlist_detail_empty_message)
            },
            style = emptyBodyStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlaylistHeader(
    playlist: Playlist?,
    artworkSize: Dp,
    useWideLayout: Boolean,
    canPlay: Boolean,
    onPlayPlaylist: () -> Unit,
    titleStyle: TextStyle,
    ownerStyle: TextStyle,
    rowSpacing: Dp,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    val playlistTitle = playlist?.name?.ifBlank {
        stringResource(R.string.playlist_detail_title)
    } ?: stringResource(R.string.playlist_detail_title)
    val owner = playlist?.owner?.trim().orEmpty()
    val buttonModifier = if (useWideLayout) Modifier else Modifier.fillMaxWidth(0.8f)
    if (useWideLayout) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(rowSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistArtwork(
                playlist = playlist,
                displaySize = artworkSize,
                requestSize = artworkSize,
                cornerRadius = 20.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Text(
                    text = playlistTitle,
                    style = titleStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (owner.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.playlist_detail_owner, owner),
                        style = ownerStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (playlist?.isEditable == true) {
                    Text(
                        text = stringResource(R.string.playlist_editable_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    FilledTonalButton(
                        onClick = onPlayPlaylist,
                        enabled = playlist != null && canPlay,
                        modifier = buttonModifier
                    ) {
                        Text(text = stringResource(R.string.playlist_detail_play))
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
            PlaylistArtwork(
                playlist = playlist,
                displaySize = artworkSize,
                requestSize = artworkSize,
                cornerRadius = 20.dp
            )
            Text(
                text = playlistTitle,
                style = titleStyle,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (owner.isNotBlank()) {
                Text(
                    text = stringResource(R.string.playlist_detail_owner, owner),
                    style = ownerStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (playlist?.isEditable == true) {
                Text(
                    text = stringResource(R.string.playlist_editable_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                FilledTonalButton(
                    onClick = onPlayPlaylist,
                    enabled = playlist != null && canPlay,
                    modifier = buttonModifier
                ) {
                    Text(text = stringResource(R.string.playlist_detail_play))
                }
            }
        }
    }
}
