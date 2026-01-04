package com.harmonixia.android.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.isLocal
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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
    onAddToFavorites: ((Track) -> Unit)? = null,
    onRemoveFromFavorites: ((Track) -> Unit)? = null,
    trackTitleTextStyle: TextStyle? = null,
    trackSupportingTextStyle: TextStyle? = null,
    trackMetadataTextStyle: TextStyle? = null,
    leadingContent: TrackListLeadingContent = TrackListLeadingContent.TrackNumber,
    imageQualityManager: ImageQualityManager? = null,
    indexProvider: ((Track, Int) -> Int)? = null,
    itemKeyProvider: ((Track, Int) -> Any)? = null,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    showEmptyState: Boolean = true,
    isReordering: Boolean = false,
    onReorder: ((Int, Int) -> Unit)? = null
) {
    var contextMenuTrackId by remember { mutableStateOf<String?>(null) }
    var contextMenuIndex by remember { mutableStateOf(-1) }
    val resolvedListState = listState ?: rememberLazyListState()
    var lastLoadTriggerIndex by remember { mutableStateOf(-1) }
    val reorderEnabled = isReordering && onReorder != null
    var draggingItemKey by remember { mutableStateOf<Any?>(null) }
    var draggingOffsetY by remember { mutableStateOf(0f) }
    val resolvedKeyProvider = itemKeyProvider ?: DefaultTrackKeyProvider
    val trackKeys = remember(tracks, resolvedKeyProvider) {
        tracks.mapIndexed { index, track -> resolvedKeyProvider(track, index) }
    }
    val trackIndexByKey = remember(trackKeys) {
        trackKeys.withIndex().associate { indexed -> indexed.value to indexed.index }
    }
    val trackIndexByKeyState = rememberUpdatedState(trackIndexByKey)
    val onReorderState = rememberUpdatedState(onReorder)
    val listStateState = rememberUpdatedState(resolvedListState)
    val tracksState = rememberUpdatedState(tracks)
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val autoScrollThresholdPx = with(density) { AUTO_SCROLL_EDGE_DP.dp.toPx() }
    val autoScrollSpeedPx = with(density) { AUTO_SCROLL_MAX_SPEED_DP.dp.toPx() }
    val context = LocalContext.current
    val artworkDisplaySize =
        imageQualityManager?.getOptimalImageSize(TrackArtworkSize) ?: TrackArtworkSize
    val artworkSizePx = with(density) { artworkDisplaySize.roundToPx() }
    val artworkBitmapConfig = imageQualityManager?.getOptimalBitmapConfig()
    val shouldPrefetchArtwork = leadingContent == TrackListLeadingContent.Artwork
    val prefetchedArtworkKeys = remember { mutableSetOf<String>() }
    val imageLoader = context.imageLoader

    LaunchedEffect(reorderEnabled) {
        if (!reorderEnabled) {
            draggingItemKey = null
            draggingOffsetY = 0f
        }
    }

    LaunchedEffect(resolvedListState, hasMore, isLoadingMore, onLoadMore) {
        if (onLoadMore == null) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = resolvedListState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to layoutInfo.totalItemsCount
        }.collect { (lastVisible, totalCount) ->
            if (!hasMore || isLoadingMore || totalCount == 0) return@collect
            if (lastVisible >= totalCount - LOAD_MORE_THRESHOLD) {
                if (lastVisible > lastLoadTriggerIndex) {
                    lastLoadTriggerIndex = lastVisible
                    onLoadMore()
                }
            }
        }
    }

    // Prefetch upcoming track artwork to warm the image cache.
    LaunchedEffect(
        resolvedListState,
        shouldPrefetchArtwork,
        artworkSizePx,
        artworkBitmapConfig
    ) {
        if (!shouldPrefetchArtwork) return@LaunchedEffect
        snapshotFlow {
            val indexByKey = trackIndexByKeyState.value
            val lastVisibleTrackIndex = resolvedListState.layoutInfo.visibleItemsInfo
                .mapNotNull { info ->
                    val key = info.key ?: return@mapNotNull null
                    indexByKey[key]
                }
                .maxOrNull() ?: -1
            lastVisibleTrackIndex
        }
            .distinctUntilChanged()
            .collect { lastVisibleTrackIndex ->
                val currentTracks = tracksState.value
                if (currentTracks.isEmpty() || lastVisibleTrackIndex < 0) return@collect
                val lastIndex = currentTracks.lastIndex
                val startIndex = (lastVisibleTrackIndex + 1).coerceAtMost(lastIndex)
                val endIndex = (lastVisibleTrackIndex + TRACK_PREFETCH_AHEAD_COUNT)
                    .coerceAtMost(lastIndex)
                if (startIndex > endIndex) return@collect
                for (index in startIndex..endIndex) {
                    val track = currentTracks.getOrNull(index) ?: continue
                    val imageUrl = track.imageUrl?.trim().orEmpty()
                    if (imageUrl.isBlank()) continue
                    val prefetchKey = buildTrackArtworkPrefetchKey(
                        imageUrl,
                        artworkSizePx,
                        artworkBitmapConfig
                    )
                    if (!prefetchedArtworkKeys.add(prefetchKey)) continue
                    imageLoader.enqueue(
                        buildTrackArtworkRequest(
                            context = context,
                            imageUrl = imageUrl,
                            sizePx = artworkSizePx,
                            bitmapConfig = artworkBitmapConfig
                        )
                    )
                }
            }
    }

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
                key = { index, track ->
                    trackKeys.getOrNull(index) ?: resolvedKeyProvider(track, index)
                }
            ) { index, track ->
                val itemKey = trackKeys.getOrNull(index) ?: resolvedKeyProvider(track, index)
                val effectiveIndex = indexProvider?.invoke(track, index) ?: index
                val displayNumber = if (track.trackNumber > 0) {
                    track.trackNumber
                } else {
                    effectiveIndex + 1
                }
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
                val isDragging = reorderEnabled && draggingItemKey == itemKey
                val placementModifier = if (reorderEnabled && !isDragging) {
                    Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                } else {
                    Modifier
                }
                val dragModifier = if (isDragging) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer { translationY = draggingOffsetY }
                } else {
                    Modifier
                }
                val handleModifier = if (reorderEnabled) {
                    Modifier
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingItemKey = itemKey
                                    draggingOffsetY = 0f
                                },
                                onDragEnd = {
                                    draggingItemKey = null
                                    draggingOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggingItemKey = null
                                    draggingOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (draggingItemKey != itemKey) return@detectDragGestures
                                    draggingOffsetY += dragAmount.y
                                    val layoutInfo = listStateState.value.layoutInfo
                                    val indexByKey = trackIndexByKeyState.value
                                    val draggedInfo = layoutInfo.visibleItemsInfo.firstOrNull {
                                        it.key == itemKey
                                    } ?: return@detectDragGestures
                                    val draggedCenter =
                                        draggedInfo.offset + draggingOffsetY + draggedInfo.size / 2f
                                    val targetInfo = layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                        val key = info.key
                                        key != null &&
                                            key != itemKey &&
                                            key in indexByKey &&
                                            draggedCenter in
                                            info.offset.toFloat()..(info.offset + info.size).toFloat()
                                    }
                                    val currentIndex = indexByKey[itemKey]
                                    val targetKey = targetInfo?.key
                                    val targetIndex = targetKey?.let { indexByKey[it] }
                                    if (currentIndex != null &&
                                        targetIndex != null &&
                                        currentIndex != targetIndex
                                    ) {
                                        val offsetDelta =
                                            (targetInfo.offset - draggedInfo.offset).toFloat()
                                        if (offsetDelta != 0f) {
                                            draggingOffsetY -= offsetDelta
                                        }
                                        onReorderState.value?.invoke(currentIndex, targetIndex)
                                    }
                                    val viewportStart = layoutInfo.viewportStartOffset
                                    val viewportEnd = layoutInfo.viewportEndOffset
                                    val listState = listStateState.value
                                    if (draggedCenter < viewportStart + autoScrollThresholdPx &&
                                        listState.canScrollBackward
                                    ) {
                                        val distance = (viewportStart + autoScrollThresholdPx - draggedCenter)
                                            .coerceAtLeast(0f)
                                        val scrollDelta = -autoScrollSpeedPx *
                                            (distance / autoScrollThresholdPx).coerceIn(0f, 1f)
                                        coroutineScope.launch {
                                            listState.scrollBy(scrollDelta)
                                        }
                                    } else if (draggedCenter > viewportEnd - autoScrollThresholdPx &&
                                        listState.canScrollForward
                                    ) {
                                        val distance = (draggedCenter - (viewportEnd - autoScrollThresholdPx))
                                            .coerceAtLeast(0f)
                                        val scrollDelta = autoScrollSpeedPx *
                                            (distance / autoScrollThresholdPx).coerceIn(0f, 1f)
                                        coroutineScope.launch {
                                            listState.scrollBy(scrollDelta)
                                        }
                                    }
                                }
                            )
                        }
                } else {
                    Modifier
                }
                Box(modifier = placementModifier.then(dragModifier)) {
                    TrackListItem(
                        track = track,
                        trackNumber = displayNumber,
                        titleTextStyle = trackTitleTextStyle,
                        supportingTextStyle = trackSupportingTextStyle,
                        metadataTextStyle = trackMetadataTextStyle,
                        leadingContent = leadingContent,
                        imageQualityManager = imageQualityManager,
                        showReorderHandle = reorderEnabled,
                        reorderHandleModifier = handleModifier,
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
                            onAddToFavorites = { onAddToFavorites?.invoke(track) },
                            onRemoveFromFavorites = { onRemoveFromFavorites?.invoke(track) },
                            isFavorite = track.isFavorite,
                            onRemoveFromPlaylist = {
                                onRemoveFromPlaylist?.invoke(track, contextMenuIndex)
                            }
                        )
                    }
                }
                if (index < tracks.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun TrackListItem(
    track: Track,
    trackNumber: Int,
    titleTextStyle: TextStyle?,
    supportingTextStyle: TextStyle?,
    metadataTextStyle: TextStyle?,
    leadingContent: TrackListLeadingContent,
    imageQualityManager: ImageQualityManager?,
    showReorderHandle: Boolean = false,
    reorderHandleModifier: Modifier = Modifier,
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
    ListItem(
        modifier = modifier,
        leadingContent = {
            if (leadingContent == TrackListLeadingContent.Artwork) {
                Box(
                    modifier = Modifier.width(TrackLeadingWidth),
                    contentAlignment = Alignment.Center
                ) {
                    TrackArtwork(
                        imageUrl = track.imageUrl,
                        imageQualityManager = imageQualityManager
                    )
                }
            } else {
                Row(
                    modifier = Modifier.width(TrackLeadingWidth),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (track.isLocal) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = "Local file",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = trackNumber.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
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
                if (showReorderHandle) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Outlined.DragHandle,
                        contentDescription = stringResource(R.string.content_desc_reorder_handle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .then(reorderHandleModifier)
                    )
                }
            }
        }
    )
}

@Composable
private fun TrackArtwork(
    imageUrl: String?,
    imageQualityManager: ImageQualityManager?,
    modifier: Modifier = Modifier
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val context = LocalContext.current
    val displaySize = imageQualityManager?.getOptimalImageSize(TrackArtworkSize) ?: TrackArtworkSize
    val sizePx = with(LocalDensity.current) { displaySize.roundToPx() }
    if (imageUrl.isNullOrBlank()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier = modifier.size(displaySize)
        ) {}
        return
    }
    val request = buildTrackArtworkRequest(
        context = context,
        imageUrl = imageUrl,
        sizePx = sizePx,
        bitmapConfig = imageQualityManager?.getOptimalBitmapConfig()
    )
    AsyncImage(
        model = request,
        contentDescription = stringResource(R.string.content_desc_album_artwork),
        placeholder = placeholder,
        error = placeholder,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(displaySize)
            .clip(MaterialTheme.shapes.small)
    )
}

private fun buildTrackArtworkRequest(
    context: Context,
    imageUrl: String,
    sizePx: Int,
    bitmapConfig: Bitmap.Config?
): ImageRequest {
    return ImageRequest.Builder(context)
        .data(imageUrl)
        .size(sizePx)
        .apply {
            if (bitmapConfig != null) {
                bitmapConfig(bitmapConfig)
            }
        }
        .build()
}

private fun buildTrackArtworkPrefetchKey(
    imageUrl: String,
    sizePx: Int,
    bitmapConfig: Bitmap.Config?
): String {
    val configName = bitmapConfig?.name ?: "default"
    return "$imageUrl:$sizePx:$configName"
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

enum class TrackListLeadingContent {
    TrackNumber,
    Artwork
}

private val TrackLeadingWidth = 40.dp
private val TrackArtworkSize = 36.dp

private const val LOAD_MORE_THRESHOLD = 10
private const val AUTO_SCROLL_EDGE_DP = 56
private const val AUTO_SCROLL_MAX_SPEED_DP = 24
private const val TRACK_PREFETCH_AHEAD_COUNT = 24
private val DefaultTrackKeyProvider: (Track, Int) -> Any = { track, _ -> track.itemId }
