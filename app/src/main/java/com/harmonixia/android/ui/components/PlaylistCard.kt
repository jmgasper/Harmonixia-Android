package com.harmonixia.android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.ui.util.PlaylistCoverEntryPoint
import com.harmonixia.android.ui.util.PlaylistCoverGenerator
import com.harmonixia.android.util.ImageQualityManager
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    isGrid: Boolean,
    gridArtworkSize: Dp = 150.dp,
    onLongClick: (() -> Unit)? = null,
    placeholderIcon: ImageVector = Icons.Outlined.QueueMusic,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier
) {
    val interactionModifier = if (onLongClick != null) {
        modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        modifier.clickable(onClick = onClick)
    }
    Card(
        modifier = interactionModifier.fillMaxWidth()
    ) {
        if (isGrid) {
            PlaylistGridContent(
                playlist = playlist,
                artworkSize = gridArtworkSize,
                placeholderIcon = placeholderIcon,
                imageQualityManager = imageQualityManager
            )
        } else {
            PlaylistListContent(
                playlist = playlist,
                placeholderIcon = placeholderIcon,
                imageQualityManager = imageQualityManager
            )
        }
    }
}

@Composable
private fun PlaylistGridContent(
    playlist: Playlist,
    artworkSize: Dp,
    placeholderIcon: ImageVector,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlaylistArtwork(
            playlist = playlist,
            size = artworkSize,
            placeholderIcon = placeholderIcon,
            imageQualityManager = imageQualityManager
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!playlist.owner.isNullOrBlank()) {
            Text(
                text = playlist.owner,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (playlist.isEditable) {
            Spacer(modifier = Modifier.height(6.dp))
            EditableBadge()
        }
    }
}

@Composable
private fun PlaylistListContent(
    playlist: Playlist,
    placeholderIcon: ImageVector,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlaylistArtwork(
            playlist = playlist,
            size = 56.dp,
            placeholderIcon = placeholderIcon,
            imageQualityManager = imageQualityManager
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!playlist.owner.isNullOrBlank()) {
                Text(
                    text = playlist.owner,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (playlist.isEditable) {
            EditableBadge()
        }
    }
}

@Composable
private fun PlaylistArtwork(
    playlist: Playlist,
    size: Dp,
    placeholderIcon: ImageVector = Icons.Outlined.QueueMusic,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val context = LocalContext.current
    val optimizedSize = imageQualityManager.getOptimalImageSize(size)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val needsGeneratedCover = playlist.imageUrl.isNullOrBlank()
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, PlaylistCoverEntryPoint::class.java)
    }
    val generator = remember(context, imageQualityManager) {
        PlaylistCoverGenerator(
            context,
            entryPoint.repository(),
            entryPoint.imageLoader(),
            imageQualityManager
        )
    }
    var coverPath by rememberSaveable(
        playlist.itemId,
        playlist.provider,
        sizePx,
        needsGeneratedCover
    ) {
        mutableStateOf<String?>(null)
    }
    LaunchedEffect(playlist.itemId, playlist.provider, sizePx, needsGeneratedCover) {
        if (!needsGeneratedCover) {
            coverPath = null
            return@LaunchedEffect
        }
        val existingPath = coverPath
        if (existingPath != null) {
            val exists = withContext(Dispatchers.IO) { File(existingPath).exists() }
            if (exists) return@LaunchedEffect
        }
        coverPath = generator.getCoverPath(playlist, sizePx)
    }
    val imageData = coverPath?.let { File(it) } ?: playlist.imageUrl

    if (imageData == null) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.size(optimizedSize)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = placeholderIcon,
                    contentDescription = stringResource(R.string.content_desc_playlist_artwork),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val imageRequest = ImageRequest.Builder(context)
            .data(imageData)
            .size(sizePx)
            .bitmapConfig(imageQualityManager.getOptimalBitmapConfig())
            .build()
        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.content_desc_playlist_artwork),
            placeholder = placeholder,
            error = placeholder,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(optimizedSize)
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

@Composable
private fun EditableBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.playlist_editable_badge),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
