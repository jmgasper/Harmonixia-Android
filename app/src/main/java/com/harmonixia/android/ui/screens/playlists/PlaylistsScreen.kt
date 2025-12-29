package com.harmonixia.android.ui.screens.playlists

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.OfflineModeBanner
import com.harmonixia.android.ui.components.PlaylistCard
import com.harmonixia.android.ui.components.PlaylistOptionsMenu
import com.harmonixia.android.ui.components.RenamePlaylistDialog
import com.harmonixia.android.ui.navigation.MainScaffoldActions
import com.harmonixia.android.ui.theme.rememberAdaptiveSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onNavigateToSettings: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRenaming by viewModel.isRenaming.collectAsStateWithLifecycle()
    val renameErrorMessageResId by viewModel.renameErrorMessageResId.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var menuPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PlaylistsUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }
                PlaylistsUiEvent.PlaylistCreated -> {
                    showCreateDialog = false
                    playlistName = ""
                }
                is PlaylistsUiEvent.PlaylistDeleted -> {
                    showDeleteDialog = false
                    deleteTarget = null
                }
                is PlaylistsUiEvent.PlaylistRenamed -> {
                    showRenameDialog = false
                    renameTarget = null
                    renameValue = ""
                }
            }
        }
    }

    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    val configuration = LocalConfiguration.current
    val spacing = rememberAdaptiveSpacing()
    val isGrid by remember(windowSizeClass) {
        derivedStateOf { windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact }
    }
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns by remember(windowSizeClass, configuration) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Medium -> if (isLandscape) 3 else 2
                WindowWidthSizeClass.Expanded -> {
                    if (isLandscape) {
                        (configuration.screenWidthDp / 160).coerceIn(2, 8)
                    } else {
                        (configuration.screenWidthDp / 180).coerceIn(2, 6)
                    }
                }
                else -> 2
            }
        }
    }
    val horizontalPadding = spacing.large
    val listVerticalPadding = if (isLandscape) spacing.medium * 0.75f else spacing.medium
    val gridVerticalPadding = if (isLandscape) spacing.large * 0.75f else spacing.large
    val listPadding = PaddingValues(horizontal = horizontalPadding, vertical = listVerticalPadding)
    val gridPadding = PaddingValues(horizontal = horizontalPadding, vertical = gridVerticalPadding)
    val gridSpacing = if (isLandscape) spacing.medium else spacing.large
    val listSpacing = if (isLandscape) spacing.medium else spacing.large

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

    val successState = uiState as? PlaylistsUiState.Success
    val lazyPagingItems = successState?.playlists?.collectAsLazyPagingItems()
    val isRefreshing = lazyPagingItems?.loadState?.refresh is LoadState.Loading

    val openRename: (Playlist) -> Unit = { target ->
        renameTarget = target
        renameValue = target.name
        viewModel.clearRenameError()
        showRenameDialog = true
    }
    val openDelete: (Playlist) -> Unit = { target ->
        deleteTarget = target
        showDeleteDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.playlists_title)) },
                actions = {
                    MainScaffoldActions()
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.action_open_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.content_desc_create_playlist)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                PlaylistsUiState.Loading -> {
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
                                text = stringResource(R.string.playlists_loading),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is PlaylistsUiState.Error -> {
                    val message = state.message.ifBlank {
                        stringResource(R.string.playlists_error)
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
                PlaylistsUiState.Empty -> {
                    PlaylistsEmptyState(
                        horizontalPadding = horizontalPadding,
                        isOfflineMode = isOfflineMode,
                        onCreatePlaylist = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                is PlaylistsUiState.Success -> {
                    val items = lazyPagingItems ?: return@Column
                    when {
                        items.loadState.refresh is LoadState.Loading -> {
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
                                        text = stringResource(R.string.playlists_loading),
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
                                            ?: stringResource(R.string.playlists_error)
                                    )
                                    Button(onClick = { items.retry() }) {
                                        Text(text = stringResource(R.string.action_retry))
                                    }
                                }
                            }
                        }
                        items.itemCount == 0 -> {
                            PlaylistsEmptyState(
                                horizontalPadding = horizontalPadding,
                                isOfflineMode = isOfflineMode,
                                onCreatePlaylist = { showCreateDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                        isGrid -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                contentPadding = gridPadding,
                                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(
                                    count = items.itemCount,
                                    key = { index ->
                                        items[index]?.let { playlist ->
                                            "${playlist.provider}:${playlist.itemId}"
                                        } ?: "placeholder_$index"
                                    }
                                ) { index ->
                                    val playlist = items[index]
                                    if (playlist != null) {
                                        Box {
                                            PlaylistCard(
                                                playlist = playlist,
                                                onClick = { onPlaylistClick(playlist) },
                                                onLongClick = {
                                                    if (playlist.isEditable) {
                                                        menuPlaylist = playlist
                                                    }
                                                },
                                                isGrid = true
                                            )
                                            if (playlist.isEditable) {
                                                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                                    IconButton(
                                                        onClick = { menuPlaylist = playlist }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.MoreVert,
                                                            contentDescription = stringResource(
                                                                R.string.playlist_options_menu
                                                            )
                                                        )
                                                    }
                                                    PlaylistOptionsMenu(
                                                        expanded = menuPlaylist == playlist,
                                                        onDismissRequest = { menuPlaylist = null },
                                                        onRename = { openRename(playlist) },
                                                        onDelete = { openDelete(playlist) }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        PlaylistCardPlaceholder(
                                            isGrid = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = listPadding,
                                verticalArrangement = Arrangement.spacedBy(listSpacing)
                            ) {
                                items(
                                    count = items.itemCount,
                                    key = { index ->
                                        items[index]?.let { playlist ->
                                            "${playlist.provider}:${playlist.itemId}"
                                        } ?: "placeholder_$index"
                                    }
                                ) { index ->
                                    val playlist = items[index]
                                    if (playlist != null) {
                                        Box {
                                            PlaylistCard(
                                                playlist = playlist,
                                                onClick = { onPlaylistClick(playlist) },
                                                onLongClick = {
                                                    if (playlist.isEditable) {
                                                        menuPlaylist = playlist
                                                    }
                                                },
                                                isGrid = false
                                            )
                                            if (playlist.isEditable) {
                                                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                                    IconButton(
                                                        onClick = { menuPlaylist = playlist }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.MoreVert,
                                                            contentDescription = stringResource(
                                                                R.string.playlist_options_menu
                                                            )
                                                        )
                                                    }
                                                    PlaylistOptionsMenu(
                                                        expanded = menuPlaylist == playlist,
                                                        onDismissRequest = { menuPlaylist = null },
                                                        onRename = { openRename(playlist) },
                                                        onDelete = { openDelete(playlist) }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        PlaylistCardPlaceholder(
                                            isGrid = false,
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

    if (showRenameDialog && renameTarget != null) {
        val errorMessage = renameErrorMessageResId?.let { stringResource(it) }
        RenamePlaylistDialog(
            currentName = renameTarget?.name.orEmpty(),
            name = renameValue,
            onNameChange = {
                renameValue = it
                viewModel.clearRenameError()
            },
            onConfirm = {
                renameTarget?.let { target ->
                    viewModel.renamePlaylist(target, renameValue)
                }
            },
            onDismiss = {
                showRenameDialog = false
                renameTarget = null
                renameValue = ""
                viewModel.clearRenameError()
            },
            isLoading = isRenaming,
            errorMessage = errorMessage
        )
    }

    if (showDeleteDialog && deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(R.string.playlist_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.playlist_delete_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        deleteTarget?.let { target ->
                            viewModel.deletePlaylist(target)
                        }
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
}

@Composable
private fun PlaylistCardPlaceholder(
    isGrid: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    Card(modifier = modifier) {
        if (isGrid) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(10.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsEmptyState(
    horizontalPadding: Dp,
    isOfflineMode: Boolean,
    onCreatePlaylist: () -> Unit,
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
            Text(
                text = if (isOfflineMode) {
                    stringResource(R.string.no_downloaded_content)
                } else {
                    stringResource(R.string.playlists_empty)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (!isOfflineMode) {
                Button(onClick = onCreatePlaylist) {
                    Text(text = stringResource(R.string.playlists_create))
                }
            }
        }
    }
}
