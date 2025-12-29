package com.harmonixia.android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.DownloadProgress
import com.harmonixia.android.domain.model.DownloadStatus
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.model.downloadId
import kotlinx.coroutines.flow.Flow

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TrackList(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    headerContent: (LazyListScope.() -> Unit)? = null,
    showContextMenu: Boolean = false,
    isEditable: Boolean = false,
    onTrackLongClick: ((Track, Int) -> Unit)? = null,
    onAddToPlaylist: ((Track) -> Unit)? = null,
    onRemoveFromPlaylist: ((Track, Int) -> Unit)? = null,
    onDownloadTrack: ((Track) -> Unit)? = null,
    getTrackDownloadStatus: ((String) -> Flow<DownloadStatus?>)? = null,
    getTrackDownloadProgress: ((String) -> Flow<DownloadProgress?>)? = null,
    trackTitleTextStyle: TextStyle? = null,
    trackSupportingTextStyle: TextStyle? = null,
    trackMetadataTextStyle: TextStyle? = null,
    indexProvider: ((Track, Int) -> Int)? = null,
    showEmptyState: Boolean = true
) {
    var contextMenuTrackId by remember { mutableStateOf<String?>(null) }
    var contextMenuIndex by remember { mutableStateOf(-1) }
    val resolvedListState = listState ?: rememberLazyListState()
    LazyColumn(
        modifier = modifier,
        state = resolvedListState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        headerContent?.invoke(this)
        if (tracks.isEmpty()) {
            if (showEmptyState) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.album_detail_no_tracks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            }
        } else {
            itemsIndexed(
                items = tracks,
                key = { _, track -> track.itemId }
            ) { index, track ->
                val effectiveIndex = indexProvider?.invoke(track, index) ?: index
                val displayNumber = if (track.trackNumber > 0) {
                    track.trackNumber
                } else {
                    effectiveIndex + 1
                }
                val downloadStatusState = getTrackDownloadStatus?.invoke(track.downloadId)
                    ?.collectAsStateWithLifecycle(initialValue = null)
                val downloadProgressState = getTrackDownloadProgress?.invoke(track.downloadId)
                    ?.collectAsStateWithLifecycle(initialValue = null)
                val downloadStatus by (downloadStatusState
                    ?: remember { mutableStateOf<DownloadStatus?>(null) })
                val downloadProgress by (downloadProgressState
                    ?: remember { mutableStateOf<DownloadProgress?>(null) })
                val isDownloaded = downloadStatus == DownloadStatus.COMPLETED
                val hasLongClick = showContextMenu || onTrackLongClick != null
                val interactionModifier = if (hasLongClick) {
                    Modifier.combinedClickable(
                        onClick = { onTrackClick(track) },
                        onLongClick = {
                            onTrackLongClick?.invoke(track, effectiveIndex)
                            if (showContextMenu) {
                                contextMenuTrackId = track.itemId
                                contextMenuIndex = effectiveIndex
                            }
                        }
                    )
                } else {
                    Modifier.clickable(onClick = { onTrackClick(track) })
                }
                Box {
                    TrackListItem(
                        track = track,
                        trackNumber = displayNumber,
                        downloadStatus = downloadStatus,
                        downloadProgress = downloadProgress,
                        titleTextStyle = trackTitleTextStyle,
                        supportingTextStyle = trackSupportingTextStyle,
                        metadataTextStyle = trackMetadataTextStyle,
                        modifier = interactionModifier
                    )
                    if (showContextMenu && contextMenuTrackId == track.itemId) {
                        TrackContextMenu(
                            expanded = true,
                            onDismissRequest = {
                                contextMenuTrackId = null
                                contextMenuIndex = -1
                            },
                            isEditable = isEditable,
                            onPlay = { onTrackClick(track) },
                            onAddToPlaylist = { onAddToPlaylist?.invoke(track) },
                            onRemoveFromPlaylist = {
                                onRemoveFromPlaylist?.invoke(track, contextMenuIndex)
                            },
                            onDownload = { onDownloadTrack?.invoke(track) },
                            isDownloaded = isDownloaded
                        )
                    }
                }
                if (index < tracks.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}

@Composable
private fun TrackListItem(
    track: Track,
    trackNumber: Int,
    downloadStatus: DownloadStatus?,
    downloadProgress: DownloadProgress?,
    titleTextStyle: TextStyle?,
    supportingTextStyle: TextStyle?,
    metadataTextStyle: TextStyle?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val durationText = formatDuration(track.lengthSeconds)
    val qualityLabel = formatTrackQualityLabel(
        track.quality,
        context::getString,
        showLosslessDetail = false
    )
    val title = if (track.title.isNotBlank()) track.title else track.uri
    val artist = if (track.artist.isNotBlank()) track.artist else track.album
    val downloadingDescription = stringResource(R.string.content_desc_track_downloading)
    ListItem(
        modifier = modifier,
        leadingContent = {
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = trackNumber.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        },
        headlineContent = {
            if (titleTextStyle != null) {
                Text(
                    text = title,
                    style = titleTextStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        supportingContent = {
            if (supportingTextStyle != null) {
                Text(
                    text = artist,
                    style = supportingTextStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                when (downloadStatus) {
                    DownloadStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = stringResource(R.string.content_desc_track_downloaded),
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .size(20.dp)
                        )
                    }
                    DownloadStatus.IN_PROGRESS -> {
                        DownloadProgressIndicator(
                            progress = downloadProgress?.progress ?: 0,
                            size = 20.dp,
                            showLabel = false,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    DownloadStatus.PENDING -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .size(20.dp)
                                .semantics {
                                    contentDescription = downloadingDescription
                                },
                            strokeWidth = 2.dp
                        )
                    }
                    else -> Unit
                }
                if (qualityLabel != null) {
                    TrackQualityBadge(
                        text = qualityLabel,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = durationText,
                    style = metadataTextStyle ?: MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
internal fun TrackQualityBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}
