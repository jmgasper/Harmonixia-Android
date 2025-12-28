package com.harmonixia.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.util.ImageQualityManager

@Composable
fun ArtistListItem(
    artist: Artist,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            leadingContent = {
                ArtistAvatar(
                    imageUrl = artist.imageUrl,
                    size = 48.dp
                )
            },
            headlineContent = {
                Text(
                    text = artist.name.ifBlank { artist.sortName.orEmpty() },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
        }
    }
}

@Composable
private fun ArtistAvatar(
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val context = LocalContext.current
    val qualityManager = remember(context) { ImageQualityManager(context) }
    val optimizedSize = qualityManager.getOptimalImageSize(size)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    if (imageUrl.isNullOrBlank()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            modifier = modifier.size(optimizedSize)
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
        val imageRequest = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(sizePx)
            .bitmapConfig(qualityManager.getOptimalBitmapConfig())
            .build()
        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.content_desc_artist_image),
            placeholder = placeholder,
            error = placeholder,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(optimizedSize)
                .clip(CircleShape)
        )
    }
}
