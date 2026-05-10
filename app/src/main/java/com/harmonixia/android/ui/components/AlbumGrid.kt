package com.harmonixia.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil3.imageLoader
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.ui.util.buildAlbumArtworkRequest
import com.harmonixia.android.util.ImageQualityManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private val AlbumGridSpacing = 8.dp
private const val PREFETCH_START_OFFSET = 20
private const val PREFETCH_END_OFFSET = 60

internal fun computeArtworkPrefetchWindow(
    firstVisibleItemIndex: Int,
    itemCount: Int,
    startOffset: Int = PREFETCH_START_OFFSET,
    endOffset: Int = PREFETCH_END_OFFSET
): IntRange? {
    if (itemCount <= 0) return null
    val lastIndex = itemCount - 1
    val startIndex = (firstVisibleItemIndex + startOffset).coerceIn(0, lastIndex)
    val endIndex = (firstVisibleItemIndex + endOffset).coerceIn(0, lastIndex)
    if (startIndex > endIndex) return null
    return startIndex..endIndex
}

@Composable
fun AlbumGrid(
    albums: LazyPagingItems<Album>,
    onAlbumClick: (Album) -> Unit,
    columns: Int,
    artworkSize: Dp,
    contentPadding: PaddingValues,
    isOfflineMode: Boolean,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier,
    onAlbumLongClick: ((Album) -> Unit)? = null,
    gridState: LazyGridState = rememberLazyGridState()
) {
    val safeColumns = columns.coerceAtLeast(1)
    val minCardHeight = artworkSize + 70.dp
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val optimizedSize = imageQualityManager.getOptimalImageSize(artworkSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val bitmapConfig = imageQualityManager.getOptimalBitmapConfig()
    LaunchedEffect(albums, gridState, sizePx, bitmapConfig, isOfflineMode) {
        snapshotFlow {
            Triple(
                gridState.firstVisibleItemIndex,
                albums.itemCount,
                albums.loadState.refresh is LoadState.Loading ||
                    albums.loadState.append is LoadState.Loading
            )
        }
            .distinctUntilChanged()
            .collectLatest { (firstVisibleItemIndex, itemCount, isLoading) ->
                if (isLoading || itemCount == 0) return@collectLatest
                val prefetchWindow = computeArtworkPrefetchWindow(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    itemCount = itemCount
                ) ?: return@collectLatest
                for (index in prefetchWindow) {
                    val album = albums[index] ?: continue
                    if (album.imageUrl.isNullOrBlank()) continue
                    imageLoader.enqueue(
                        buildAlbumArtworkRequest(
                            context = context,
                            album = album,
                            sizePx = sizePx,
                            bitmapConfig = bitmapConfig,
                            isOfflineMode = isOfflineMode
                        )
                    )
                }
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(safeColumns),
        state = gridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(AlbumGridSpacing),
        verticalArrangement = Arrangement.spacedBy(AlbumGridSpacing),
        modifier = modifier.fillMaxWidth()
    ) {
        items(
            count = albums.itemCount,
            key = { index ->
                albums[index]?.let { album -> "${album.provider}:${album.itemId}" }
                    ?: "placeholder_$index"
            }
        ) { index ->
            val album = albums[index]
            if (album != null) {
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) },
                    onLongClick = onAlbumLongClick?.let { callback -> { callback(album) } },
                    artworkSize = artworkSize,
                    isOfflineMode = isOfflineMode,
                    imageQualityManager = imageQualityManager,
                    modifier = Modifier.heightIn(min = minCardHeight)
                )
            } else {
                AlbumCardPlaceholder(
                    artworkSize = artworkSize,
                    modifier = Modifier.heightIn(min = minCardHeight)
                )
            }
        }
    }
}

@Composable
fun AlbumGridStatic(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    columns: Int,
    artworkSize: Dp,
    contentPadding: PaddingValues,
    isOfflineMode: Boolean,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier,
    onAlbumLongClick: ((Album) -> Unit)? = null,
    gridState: LazyGridState = rememberLazyGridState()
) {
    val safeColumns = columns.coerceAtLeast(1)
    val minCardHeight = artworkSize + 70.dp
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val optimizedSize = imageQualityManager.getOptimalImageSize(artworkSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val bitmapConfig = imageQualityManager.getOptimalBitmapConfig()

    LaunchedEffect(albums.size, gridState, sizePx, bitmapConfig, isOfflineMode) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { firstVisibleItemIndex ->
                if (albums.isEmpty()) return@collectLatest
                val prefetchWindow = computeArtworkPrefetchWindow(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    itemCount = albums.size
                ) ?: return@collectLatest
                for (index in prefetchWindow) {
                    val album = albums[index]
                    if (album.imageUrl.isNullOrBlank()) continue
                    imageLoader.enqueue(
                        buildAlbumArtworkRequest(
                            context = context,
                            album = album,
                            sizePx = sizePx,
                            bitmapConfig = bitmapConfig,
                            isOfflineMode = isOfflineMode
                        )
                    )
                }
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(safeColumns),
        state = gridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(AlbumGridSpacing),
        verticalArrangement = Arrangement.spacedBy(AlbumGridSpacing),
        modifier = modifier.fillMaxWidth()
    ) {
        items(
            items = albums,
            key = { album -> "${album.provider}:${album.itemId}" }
        ) { album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album) },
                onLongClick = onAlbumLongClick?.let { callback -> { callback(album) } },
                artworkSize = artworkSize,
                isOfflineMode = isOfflineMode,
                imageQualityManager = imageQualityManager,
                modifier = Modifier.heightIn(min = minCardHeight)
            )
        }
    }
}

@Composable
private fun AlbumCardPlaceholder(
    artworkSize: Dp,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .sizeIn(maxWidth = artworkSize, maxHeight = artworkSize)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}
