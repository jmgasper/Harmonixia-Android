package com.harmonixia.android.ui.util

import android.content.Context
import android.graphics.Bitmap
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.domain.model.Album
import java.io.File

fun buildAlbumArtworkRequest(
    context: Context,
    album: Album?,
    sizePx: Int,
    bitmapConfig: Bitmap.Config
): ImageRequest {
    val data = album?.imageUrl
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { url ->
            val file = File(url)
            if (file.exists()) file else url
        }
    val builder = ImageRequest.Builder(context)
        .data(data)
        .size(sizePx)
        .bitmapConfig(bitmapConfig)

    val cacheKey = album
        ?.takeIf { !it.imageUrl.isNullOrBlank() }
        ?.let { "album_artwork:${it.provider}:${it.itemId}" }
    if (cacheKey != null) {
        builder.diskCacheKey(cacheKey)
        builder.memoryCacheKey("$cacheKey:$sizePx:${bitmapConfig.name}")
    }

    return builder.build()
}
