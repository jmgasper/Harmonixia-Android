package com.harmonixia.android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.ui.util.buildAlbumArtworkRequest
import com.harmonixia.android.util.ImageQualityManager

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    artworkSize: Dp = 150.dp,
    isOfflineMode: Boolean,
    modifier: Modifier = Modifier
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val interactionModifier = if (onLongClick != null) {
        modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        modifier.clickable(onClick = onClick)
    }
    val context = LocalContext.current
    val qualityManager = remember(context) { ImageQualityManager(context) }
    val optimizedSize = qualityManager.getOptimalImageSize(artworkSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val bitmapConfig = qualityManager.getOptimalBitmapConfig()
    val imageRequest = buildAlbumArtworkRequest(
        context = context,
        album = album,
        sizePx = sizePx,
        bitmapConfig = bitmapConfig,
        isOfflineMode = isOfflineMode
    )

    Card(
        modifier = interactionModifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.content_desc_album_artwork),
                modifier = Modifier
                    .sizeIn(maxWidth = optimizedSize, maxHeight = optimizedSize)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = album.artists.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
