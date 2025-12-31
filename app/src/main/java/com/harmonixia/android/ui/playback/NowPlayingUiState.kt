package com.harmonixia.android.ui.playback

import androidx.media3.common.MediaItem
import com.harmonixia.android.domain.model.PlaybackState
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.util.EXTRA_TRACK_QUALITY

sealed class NowPlayingUiState {
    data class Loading(val info: PlaybackInfo) : NowPlayingUiState()
    data class Playing(val info: PlaybackInfo) : NowPlayingUiState()
    data object Idle : NowPlayingUiState()
}

data class PlaybackInfo(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val quality: String?,
    val duration: Long,
    val currentPosition: Long,
    val isPlaying: Boolean,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val repeatMode: RepeatMode,
    val shuffle: Boolean
)

fun buildNowPlayingUiState(
    mediaItem: MediaItem?,
    playbackState: PlaybackState,
    currentPosition: Long,
    duration: Long,
    hasNext: Boolean,
    hasPrevious: Boolean,
    repeatMode: RepeatMode,
    shuffle: Boolean
): NowPlayingUiState {
    val safePosition = currentPosition.coerceAtLeast(0L)
    val safeDuration = duration.coerceAtLeast(0L)
    if (mediaItem == null) {
        return NowPlayingUiState.Idle
    }
    val info = mediaItem.toPlaybackInfo(
        playbackState = playbackState,
        currentPosition = safePosition,
        duration = safeDuration,
        hasNext = hasNext,
        hasPrevious = hasPrevious,
        repeatMode = repeatMode,
        shuffle = shuffle
    )
    return if (playbackState == PlaybackState.IDLE) {
        NowPlayingUiState.Loading(info)
    } else {
        NowPlayingUiState.Playing(info)
    }
}

fun MediaItem.toPlaybackInfo(
    playbackState: PlaybackState,
    currentPosition: Long,
    duration: Long,
    hasNext: Boolean,
    hasPrevious: Boolean,
    repeatMode: RepeatMode,
    shuffle: Boolean
): PlaybackInfo {
    val metadata = mediaMetadata
    return PlaybackInfo(
        title = metadata.title?.toString().orEmpty(),
        artist = metadata.artist?.toString().orEmpty(),
        album = metadata.albumTitle?.toString().orEmpty(),
        artworkUrl = metadata.artworkUri?.toString(),
        quality = metadata.extras?.getString(EXTRA_TRACK_QUALITY),
        duration = duration,
        currentPosition = currentPosition,
        isPlaying = playbackState == PlaybackState.PLAYING,
        hasNext = hasNext,
        hasPrevious = hasPrevious,
        repeatMode = repeatMode,
        shuffle = shuffle
    )
}
