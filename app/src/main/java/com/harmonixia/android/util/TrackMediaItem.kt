package com.harmonixia.android.util

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.harmonixia.android.domain.model.Track
import java.io.File

fun Track.toPlaybackMediaItem(): MediaItem {
    val durationMs = lengthSeconds
        .takeIf { it > 0 }
        ?.toLong()
        ?.times(1000L)
    val extras = Bundle().apply {
        if (!quality.isNullOrBlank()) {
            putString(EXTRA_TRACK_QUALITY, quality)
        }
        putString(EXTRA_STREAM_URI, uri)
        putBoolean(EXTRA_IS_LOCAL_FILE, isLocal)
        putProviderExtras(provider, providerMappings)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(if (title.isNotBlank()) title else uri)
        .setArtist(if (artist.isNotBlank()) artist else album)
        .setAlbumTitle(album)
        .setArtworkUri(imageUrl?.let { Uri.parse(it) })
        .setDurationMs(durationMs)
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setExtras(extras)
        .build()
    val resolvedUri = if (isLocal) {
        Uri.fromFile(File(uri))
    } else {
        Uri.parse(uri)
    }
    return MediaItem.Builder()
        .setMediaId(itemId)
        .setUri(resolvedUri)
        .setMediaMetadata(metadata)
        .build()
}
