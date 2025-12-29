package com.harmonixia.android.ui.screens.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.DownloadProgress
import com.harmonixia.android.domain.model.DownloadStatus
import com.harmonixia.android.domain.model.DownloadedTrack
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.model.downloadId
import com.harmonixia.android.ui.components.AlbumCard
import com.harmonixia.android.ui.components.DownloadProgressIndicator
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.PlaylistCard
import com.harmonixia.android.ui.components.TrackList
import com.harmonixia.android.ui.theme.rememberAdaptiveSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onTrackClick: (Track) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val spacing = rememberAdaptiveSpacing()
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.downloads_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
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
                DownloadsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                DownloadsUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.downloads_empty),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.downloads_empty_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is DownloadsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorCard(message = state.message)
                    }
                }
                is DownloadsUiState.Success -> {
                    val hasDownloads = state.inProgressDownloads.isNotEmpty() ||
                        state.downloadedPlaylists.isNotEmpty() ||
                        state.downloadedAlbums.isNotEmpty() ||
                        state.downloadedTracks.isNotEmpty()
                    val trackRowHeight = 76.dp
                    val trackListHeight = trackRowHeight * state.downloadedTracks.size

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = spacing.large,
                            vertical = spacing.large
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.large)
                    ) {
                        if (state.inProgressDownloads.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.downloads_in_progress),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            items(
                                items = state.inProgressDownloads,
                                key = { item -> item.download.track.downloadId }
                            ) { item ->
                                DownloadItemCard(
                                    download = item.download,
                                    progress = item.progress,
                                    onCancel = {
                                        viewModel.cancelDownload(item.download.track.downloadId)
                                    },
                                    onPause = {
                                        viewModel.pauseDownload(item.download.track.downloadId)
                                    },
                                    onResume = {
                                        viewModel.resumeDownload(item.download.track.downloadId)
                                    }
                                )
                            }
                        }

                        if (
                            state.downloadedPlaylists.isNotEmpty() ||
                            state.downloadedAlbums.isNotEmpty() ||
                            state.downloadedTracks.isNotEmpty()
                        ) {
                            item {
                                Text(
                                    text = stringResource(R.string.downloads_completed),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        if (state.downloadedPlaylists.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.downloads_section_playlists),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            items(
                                items = state.downloadedPlaylists,
                                key = { playlist -> "${playlist.provider}:${playlist.itemId}" }
                            ) { playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist) },
                                    isGrid = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (state.downloadedAlbums.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.downloads_section_albums),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            items(
                                items = state.downloadedAlbums,
                                key = { album -> "${album.provider}:${album.itemId}" }
                            ) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album) },
                                    artworkSize = 140.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (state.downloadedTracks.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.downloads_section_tracks),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            item {
                                TrackList(
                                    tracks = state.downloadedTracks,
                                    onTrackClick = onTrackClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(trackListHeight),
                                    contentPadding = PaddingValues(0.dp),
                                    showContextMenu = false,
                                    showEmptyState = false,
                                    getTrackDownloadStatus = viewModel::getTrackDownloadStatus
                                )
                            }
                        }

                        if (hasDownloads) {
                            item {
                                Button(
                                    onClick = { showClearDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = stringResource(R.string.downloads_clear_all))
                                }
                            }
                        }
                    }
                }
            }
            if (showClearDialog) {
                ClearAllDownloadsDialog(
                    onConfirm = {
                        viewModel.clearAllDownloads()
                        showClearDialog = false
                    },
                    onDismiss = { showClearDialog = false }
                )
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    download: DownloadedTrack,
    progress: DownloadProgress,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    val title = download.track.title.ifBlank { download.track.uri }
    val subtitleParts = listOfNotNull(
        download.track.artist.takeIf { it.isNotBlank() },
        download.track.album.takeIf { it.isNotBlank() }
    )
    val subtitle = subtitleParts.joinToString(" • ")
    val statusLabel = when (download.downloadStatus) {
        DownloadStatus.PENDING -> stringResource(R.string.downloads_status_pending)
        DownloadStatus.IN_PROGRESS -> stringResource(R.string.downloads_status_in_progress)
        DownloadStatus.PAUSED -> stringResource(R.string.downloads_status_paused)
        DownloadStatus.COMPLETED -> stringResource(R.string.downloads_status_in_progress)
        DownloadStatus.FAILED -> stringResource(R.string.downloads_status_paused)
    }
    val showResume = download.downloadStatus == DownloadStatus.PAUSED
    val showPause = download.downloadStatus == DownloadStatus.IN_PROGRESS ||
        download.downloadStatus == DownloadStatus.PENDING

    ListItem(
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            DownloadProgressIndicator(
                progress = progress.progress,
                size = 48.dp
            )
        },
        headlineContent = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (download.downloadStatus == DownloadStatus.IN_PROGRESS) {
                        DownloadSpeedText(speedBps = progress.downloadSpeedBps)
                    }
                }
            }
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                if (showPause) {
                    TextButton(onClick = onPause) {
                        Text(text = stringResource(R.string.action_pause_download))
                    }
                } else if (showResume) {
                    TextButton(onClick = onResume) {
                        Text(text = stringResource(R.string.action_resume_download))
                    }
                }
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(R.string.action_cancel_download))
                }
            }
        }
    )
}

@Composable
private fun DownloadSpeedText(
    speedBps: Long,
    modifier: Modifier = Modifier
) {
    val display = formatDownloadSpeed(speedBps) ?: return
    val text = when (display.unit) {
        SpeedUnit.KBPS -> stringResource(R.string.downloads_speed_kbps, display.value)
        SpeedUnit.MBPS -> stringResource(R.string.downloads_speed_mbps, display.value)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun ClearAllDownloadsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(text = stringResource(R.string.downloads_clear_confirm_title)) },
        text = { Text(text = stringResource(R.string.downloads_clear_confirm_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(text = stringResource(R.string.downloads_clear_all))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}
