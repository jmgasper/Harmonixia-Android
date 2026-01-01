package com.harmonixia.android.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.request.bitmapConfig
import com.harmonixia.android.domain.model.Album
import java.io.File

fun buildAlbumArtworkRequest(
    context: Context,
    album: Album?,
    sizePx: Int,
    bitmapConfig: Bitmap.Config,
    isOfflineMode: Boolean
): ImageRequest {
    val data = album?.imageUrl
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
    val builder = ImageRequest.Builder(context)
        .data(data)
        .size(sizePx)
        .bitmapConfig(bitmapConfig)

    if (isOfflineMode) {
        builder.networkCachePolicy(CachePolicy.DISABLED)
        builder.diskCachePolicy(CachePolicy.READ_ONLY)
    }

    val cacheKey = buildAlbumArtworkCacheKey(album)
    if (cacheKey != null) {
        builder.diskCacheKey(cacheKey)
        builder.memoryCacheKey("$cacheKey:$sizePx:${bitmapConfig.name}")
    }

    return builder.build()
}

private fun buildAlbumArtworkCacheKey(album: Album?): String? {
    if (album == null) return null
    val name = album.name.trim()
    if (name.isBlank()) {
        val itemId = album.itemId.trim()
        val provider = album.provider.trim()
        return if (itemId.isNotBlank() && provider.isNotBlank()) {
            "album_artwork:$provider:$itemId"
        } else {
            null
        }
    }
    val artist = album.artists.firstOrNull()?.trim().orEmpty()
    val normalizedName = name.lowercase()
    val normalizedArtist = artist.lowercase()
    return if (normalizedArtist.isNotBlank()) {
        "album_artwork:$normalizedArtist:$normalizedName"
    } else {
        "album_artwork:$normalizedName"
    }
}
