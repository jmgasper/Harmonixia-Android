package com.harmonixia.android.ui.components

import android.net.Uri
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
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.R
import com.harmonixia.android.util.ImageQualityManager
import java.io.File

@Composable
fun RecommendationCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    artworkSize: Dp,
    imageQualityManager: ImageQualityManager,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val interactionModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    val context = LocalContext.current
    val optimizedSize = imageQualityManager.getOptimalImageSize(artworkSize)
    val sizePx = with(LocalDensity.current) { optimizedSize.roundToPx() }
    val bitmapConfig = imageQualityManager.getOptimalBitmapConfig()
    val data = imageUrl
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { url ->
            if (url.startsWith("content://") || url.startsWith("file://")) {
                url
            } else {
                val file = File(url)
                if (file.exists()) Uri.fromFile(file) else url
            }
        }
    val imageRequest = ImageRequest.Builder(context)
        .data(data)
        .size(sizePx)
        .bitmapConfig(bitmapConfig)
        .build()

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
                contentDescription = stringResource(R.string.content_desc_recommendation_artwork),
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
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
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
}
