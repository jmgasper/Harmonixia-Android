package com.harmonixia.android.ui.util

import android.content.Context
import android.graphics.Bitmap
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import com.harmonixia.android.domain.model.Album

fun buildAlbumArtworkRequest(
    context: Context,
    album: Album?,
    sizePx: Int,
    bitmapConfig: Bitmap.Config
): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(album?.imageUrl)
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
