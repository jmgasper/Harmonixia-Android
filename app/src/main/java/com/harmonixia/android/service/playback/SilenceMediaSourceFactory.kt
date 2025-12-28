package com.harmonixia.android.service.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SilenceMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

@UnstableApi
class SilenceMediaSourceFactory : MediaSource.Factory {
    override fun setDrmSessionManagerProvider(
        drmSessionManagerProvider: DrmSessionManagerProvider
    ): MediaSource.Factory {
        return this
    }

    override fun setLoadErrorHandlingPolicy(
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy
    ): MediaSource.Factory {
        return this
    }

    override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_OTHER)

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val durationMs = mediaItem.mediaMetadata.durationMs
        val durationUs = when {
            durationMs != null && durationMs > 0 -> durationMs * 1000L
            else -> DEFAULT_DURATION_US
        }
        val source = SilenceMediaSource.Factory()
            .setDurationUs(durationUs.coerceAtLeast(1L))
            .createMediaSource()
        source.updateMediaItem(mediaItem)
        return source
    }

    companion object {
        private const val DEFAULT_DURATION_US = 60L * 60L * 1_000_000L
    }
}
