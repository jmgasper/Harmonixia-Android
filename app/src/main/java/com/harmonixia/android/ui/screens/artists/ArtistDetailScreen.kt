package com.harmonixia.android.ui.screens.artists

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.ui.components.AlbumGridStatic
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.OfflineModeBanner
import com.harmonixia.android.ui.components.PlaylistPickerDialog
import com.harmonixia.android.ui.screens.settings.SettingsTab
import com.harmonixia.android.ui.screens.playlists.CreatePlaylistDialog
import com.harmonixia.android.ui.theme.rememberAdaptiveSpacing
import com.harmonixia.android.util.ImageQualityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: (SettingsTab?) -> Unit,
    onAlbumClick: (Album) -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showPlaylistPicker by remember { mutableStateOf(false) }
    var pendingAlbum by remember { mutableStateOf<Album?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ArtistDetailUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }
                ArtistDetailUiEvent.PlaylistCreated -> {
                    showCreateDialog = false
                    playlistName = ""
                    if (pendingAlbum != null) {
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
    val columns by remember(windowSizeClass, configuration, isLandscape) {
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
                else -> if (isLandscape) 3 else 2
            }
        }
    }
    val horizontalPadding = spacing.large
    val gridVerticalPadding = if (isLandscape) spacing.large * 0.75f else spacing.large
    val avatarSize by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 120.dp
                WindowWidthSizeClass.Medium -> 140.dp
                WindowWidthSizeClass.Expanded -> 160.dp
                else -> 120.dp
            }
        }
    }
    val artworkSize = if (isLandscape) 140.dp else 150.dp
    val gridPadding = PaddingValues(horizontal = horizontalPadding, vertical = gridVerticalPadding)
    val sectionHeaderStyle = if (isExpanded) {
        MaterialTheme.typography.headlineSmall
    } else {
        MaterialTheme.typography.titleLarge
    }
    val artistTitleStyle = if (isExpanded) {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.headlineSmall
    }
    val metaStyle = if (isExpanded) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.bodyMedium
    }

    val titleText = artist?.name?.ifBlank {
        stringResource(R.string.artist_detail_title)
    } ?: stringResource(R.string.artist_detail_title)

    Scaffold(
        topBar = {
            Column {
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
            ArtistDetailUiState.Loading -> {
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
                                text = stringResource(R.string.artist_detail_loading_albums),
                                style = MaterialTheme.typography.bodyMedium
                            )
                    }
                }
            }
            is ArtistDetailUiState.Error -> {
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
                        Button(onClick = viewModel::loadArtistAlbums) {
                            Text(text = stringResource(R.string.action_retry))
                        }
                    }
                }
            }
            ArtistDetailUiState.Empty -> {
                ArtistDetailContent(
                    artist = artist,
                    albums = emptyList(),
                    isOfflineMode = isOfflineMode,
                    columns = columns,
                    horizontalPadding = horizontalPadding,
                    gridPadding = gridPadding,
                    avatarSize = avatarSize,
                    artworkSize = artworkSize,
                    sectionHeaderStyle = sectionHeaderStyle,
                    titleStyle = artistTitleStyle,
                    metaStyle = metaStyle,
                    onAlbumClick = onAlbumClick,
                    onAlbumLongClick = { album ->
                        pendingAlbum = album
                        viewModel.refreshPlaylists()
                        showPlaylistPicker = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is ArtistDetailUiState.Success -> {
                ArtistDetailContent(
                    artist = state.artist,
                    albums = state.albums,
                    isOfflineMode = isOfflineMode,
                    columns = columns,
                    horizontalPadding = horizontalPadding,
                    gridPadding = gridPadding,
                    avatarSize = avatarSize,
                    artworkSize = artworkSize,
                    sectionHeaderStyle = sectionHeaderStyle,
                    titleStyle = artistTitleStyle,
                    metaStyle = metaStyle,
                    onAlbumClick = onAlbumClick,
                    onAlbumLongClick = { album ->
                        pendingAlbum = album
                        viewModel.refreshPlaylists()
                        showPlaylistPicker = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }

    if (showPlaylistPicker) {
        val album = pendingAlbum
        if (album != null) {
            PlaylistPickerDialog(
                playlists = playlists,
                onPlaylistSelected = { playlist ->
                    viewModel.addAlbumToPlaylist(album, playlist)
                    showPlaylistPicker = false
                    pendingAlbum = null
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
private fun ArtistDetailContent(
    artist: Artist?,
    albums: List<Album>,
    isOfflineMode: Boolean,
    columns: Int,
    horizontalPadding: Dp,
    gridPadding: PaddingValues,
    avatarSize: Dp,
    artworkSize: Dp,
    sectionHeaderStyle: androidx.compose.ui.text.TextStyle,
    titleStyle: androidx.compose.ui.text.TextStyle,
    metaStyle: androidx.compose.ui.text.TextStyle,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ArtistHeader(
            artist = artist,
            albumCount = albums.size,
            avatarSize = avatarSize,
            horizontalPadding = horizontalPadding,
            titleStyle = titleStyle,
            metaStyle = metaStyle
        )
        Spacer(modifier = Modifier.height(spacing.large))
        if (albums.isEmpty()) {
            ArtistAlbumsEmptyState(
                isOfflineMode = isOfflineMode,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                textStyle = metaStyle
            )
        } else {
            Text(
                text = stringResource(R.string.artist_detail_albums),
                style = sectionHeaderStyle,
                modifier = Modifier.padding(horizontal = horizontalPadding)
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            AlbumGridStatic(
                albums = albums,
                onAlbumClick = onAlbumClick,
                onAlbumLongClick = onAlbumLongClick,
                columns = columns,
                artworkSize = artworkSize,
                contentPadding = gridPadding,
                isOfflineMode = isOfflineMode,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ArtistHeader(
    artist: Artist?,
    albumCount: Int,
    avatarSize: Dp,
    horizontalPadding: Dp,
    titleStyle: androidx.compose.ui.text.TextStyle,
    metaStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val context = LocalContext.current
    val qualityManager = remember(context) { ImageQualityManager(context) }
    val optimizedSize = qualityManager.getOptimalImageSize(avatarSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val name = artist?.name?.ifBlank {
        stringResource(R.string.artist_detail_title)
    } ?: stringResource(R.string.artist_detail_title)
    val imageUrl = artist?.imageUrl
    val imageRequest = ImageRequest.Builder(context)
        .data(imageUrl)
        .size(sizePx)
        .bitmapConfig(qualityManager.getOptimalBitmapConfig())
        .build()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        if (imageUrl.isNullOrBlank()) {
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
            text = name,
            style = titleStyle,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.artist_detail_album_count, albumCount),
            style = metaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ArtistAlbumsEmptyState(
    isOfflineMode: Boolean,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    Box(
        modifier = modifier.fillMaxSize(),
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
                    stringResource(R.string.artist_detail_no_albums)
                },
                style = textStyle,
                textAlign = TextAlign.Center
            )
        }
    }
}
