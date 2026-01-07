package com.harmonixia.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.ui.util.buildAlbumArtworkRequest
import com.harmonixia.android.util.ImageQualityManager

@Composable
fun AlbumListItem(
    album: Album,
    onClick: () -> Unit,
    showDivider: Boolean,
    isOfflineMode: Boolean,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier
) {
    val artistText = album.artists.joinToString(", ").trim()
    val albumName = album.name.trim()
    val headlineText = if (artistText.isNotBlank()) artistText else albumName
    val showAlbumName = artistText.isNotBlank() && albumName.isNotBlank()
    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            leadingContent = {
                AlbumArtwork(
                    album = album,
                    size = 56.dp,
                    isOfflineMode = isOfflineMode,
                    imageQualityManager = imageQualityManager
                )
            },
            headlineContent = {
                Text(
                    text = headlineText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (showAlbumName) {
                        Text(
                            text = albumName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = stringResource(R.string.library_track_count, album.trackCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
        }
    }
}

@Composable
fun AlbumListItemPlaceholder(
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            },
            headlineContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(10.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(10.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        )
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
        }
    }
}

@Composable
private fun AlbumArtwork(
    album: Album,
    size: Dp,
    isOfflineMode: Boolean,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val context = LocalContext.current
    val optimizedSize = imageQualityManager.getOptimalImageSize(size)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
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
            .size(optimizedSize)
            .clip(RoundedCornerShape(8.dp))
    )
}
