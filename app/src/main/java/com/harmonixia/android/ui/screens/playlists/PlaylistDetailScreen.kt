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
import androidx.compose.runtime.mutableStateOf
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
import com.harmonixia.android.ui.components.PlaylistOptionsMenu
import com.harmonixia.android.ui.components.PlaylistPickerDialog
import com.harmonixia.android.ui.components.RenamePlaylistDialog
import com.harmonixia.android.ui.components.TrackList
import com.harmonixia.android.ui.theme.rememberAdaptiveSpacing
import com.harmonixia.android.util.ImageQualityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlaylist: (Playlist) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isRenaming by viewModel.isRenaming.collectAsStateWithLifecycle()
    val renameErrorMessageResId by viewModel.renameErrorMessageResId.collectAsStateWithLifecycle()
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

    val titleText = playlist?.name?.ifBlank {
        stringResource(R.string.playlist_detail_title)
    } ?: stringResource(R.string.playlist_detail_title)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    if (playlist?.isEditable == true) {
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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.action_open_settings)
                        )
                    }
                }
            )
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
                    useWideLayout = useWideLayout,
                    horizontalPadding = horizontalPadding,
                    isVeryWide = isVeryWide,
                    titleStyle = playlistTitleStyle,
                    ownerStyle = ownerStyle,
                    sectionHeaderStyle = sectionHeaderStyle,
                    trackTitleStyle = trackTitleStyle,
                    trackMetaStyle = trackMetaStyle,
                    rowSpacing = if (isExpanded) 32.dp else 24.dp,
                    onPlayPlaylist = { viewModel.playPlaylist() },
                    onTrackClick = viewModel::playTrack,
                    onAddToPlaylist = { track ->
                        pendingTrack = track
                        viewModel.refreshPlaylists()
                        showPlaylistPicker = true
                    },
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
    useWideLayout: Boolean,
    horizontalPadding: Dp,
    isVeryWide: Boolean,
    titleStyle: TextStyle,
    ownerStyle: TextStyle,
    sectionHeaderStyle: TextStyle,
    trackTitleStyle: TextStyle?,
    trackMetaStyle: TextStyle?,
    rowSpacing: Dp,
    onPlayPlaylist: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
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
    val contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = spacing.large)

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
        TrackList(
            tracks = tracks,
            onTrackClick = onTrackClick,
            showContextMenu = true,
            isEditable = playlist?.isEditable == true,
            onAddToPlaylist = onAddToPlaylist,
            onRemoveFromPlaylist = onRemoveFromPlaylist,
            modifier = modifier,
            contentPadding = contentPadding,
            headerContent = {
                item {
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

@Composable
private fun PlaylistDetailEmptyContent(
    playlist: Playlist?,
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
            text = stringResource(R.string.playlist_detail_no_tracks),
            style = emptyTitleStyle,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        Text(
            text = stringResource(R.string.playlist_detail_empty_message),
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
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val context = LocalContext.current
    val qualityManager = remember(context) { ImageQualityManager(context) }
    val optimizedSize = qualityManager.getOptimalImageSize(artworkSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val imageRequest = ImageRequest.Builder(context)
        .data(playlist?.imageUrl)
        .size(sizePx)
        .bitmapConfig(qualityManager.getOptimalBitmapConfig())
        .build()
    val playlistTitle = playlist?.name?.ifBlank {
        stringResource(R.string.playlist_detail_title)
    } ?: stringResource(R.string.playlist_detail_title)
    val owner = playlist?.owner?.trim().orEmpty()

    if (useWideLayout) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(rowSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.content_desc_playlist_artwork),
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(optimizedSize)
                    .clip(RoundedCornerShape(20.dp))
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
                FilledTonalButton(
                    onClick = onPlayPlaylist,
                    enabled = playlist != null && canPlay
                ) {
                    Text(text = stringResource(R.string.playlist_detail_play))
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.content_desc_playlist_artwork),
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(optimizedSize)
                    .clip(RoundedCornerShape(20.dp))
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
            FilledTonalButton(
                onClick = onPlayPlaylist,
                enabled = playlist != null && canPlay,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(text = stringResource(R.string.playlist_detail_play))
            }
        }
    }
}
