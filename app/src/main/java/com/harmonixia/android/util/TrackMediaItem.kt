package com.harmonixia.android.util

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.harmonixia.android.domain.model.Track
import java.io.File

fun Track.playbackDurationMs(): Long? {
    return lengthSeconds
        .takeIf { it > 0 }
        ?.toLong()
        ?.times(1000L)
}

fun Track.buildPlaybackExtras(
    isLocalFile: Boolean,
    parentMediaId: String? = null
): Bundle {
    return Bundle().apply {
        if (!quality.isNullOrBlank()) {
            putString(EXTRA_TRACK_QUALITY, quality)
        }
        putBoolean(EXTRA_IS_LOCAL_FILE, isLocalFile)
        putString(EXTRA_STREAM_URI, uri)
        putProviderExtras(provider, providerMappings)
        if (!parentMediaId.isNullOrBlank()) {
            putString(EXTRA_PARENT_MEDIA_ID, parentMediaId)
        }
    }
}

fun Track.toPlaybackMediaItem(): MediaItem {
    val durationMs = playbackDurationMs()
    val extras = buildPlaybackExtras(isLocalFile = isLocal)
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
