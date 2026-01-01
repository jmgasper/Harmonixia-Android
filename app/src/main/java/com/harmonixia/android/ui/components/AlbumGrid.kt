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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

private val AlbumGridSpacing = 8.dp
private const val PREFETCH_START_OFFSET = 10
private const val PREFETCH_END_OFFSET = 30

@Composable
fun AlbumGrid(
    albums: LazyPagingItems<Album>,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: ((Album) -> Unit)? = null,
    columns: Int,
    artworkSize: Dp,
    contentPadding: PaddingValues,
    isOfflineMode: Boolean,
    modifier: Modifier = Modifier
) {
    val safeColumns = columns.coerceAtLeast(1)
    val minCardHeight = artworkSize + 70.dp
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val qualityManager = remember(context) { ImageQualityManager(context) }
    val optimizedSize = qualityManager.getOptimalImageSize(artworkSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val bitmapConfig = qualityManager.getOptimalBitmapConfig()
    val isLoading = albums.loadState.refresh is LoadState.Loading ||
        albums.loadState.append is LoadState.Loading

    LaunchedEffect(albums.itemCount, gridState.firstVisibleItemIndex, isLoading, sizePx, bitmapConfig) {
        if (isLoading || albums.itemCount == 0) return@LaunchedEffect
        val startIndex = (gridState.firstVisibleItemIndex + PREFETCH_START_OFFSET)
            .coerceAtMost(albums.itemCount - 1)
        val endIndex = (gridState.firstVisibleItemIndex + PREFETCH_END_OFFSET)
            .coerceAtMost(albums.itemCount - 1)
        if (startIndex > endIndex) return@LaunchedEffect
        for (index in startIndex..endIndex) {
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
    onAlbumLongClick: ((Album) -> Unit)? = null,
    columns: Int,
    artworkSize: Dp,
    contentPadding: PaddingValues,
    isOfflineMode: Boolean,
    modifier: Modifier = Modifier
) {
    val safeColumns = columns.coerceAtLeast(1)
    val minCardHeight = artworkSize + 70.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(safeColumns),
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
