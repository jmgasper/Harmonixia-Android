package com.harmonixia.android.ui.playback

import androidx.media3.common.MediaItem
import com.harmonixia.android.domain.model.PlaybackState

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
    val duration: Long,
    val currentPosition: Long,
    val isPlaying: Boolean,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

fun buildNowPlayingUiState(
    mediaItem: MediaItem?,
    playbackState: PlaybackState,
    currentPosition: Long,
    duration: Long,
    hasNext: Boolean,
    hasPrevious: Boolean
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
        hasPrevious = hasPrevious
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
    hasPrevious: Boolean
): PlaybackInfo {
    val metadata = mediaMetadata
    return PlaybackInfo(
        title = metadata.title?.toString().orEmpty(),
        artist = metadata.artist?.toString().orEmpty(),
        album = metadata.albumTitle?.toString().orEmpty(),
        artworkUrl = metadata.artworkUri?.toString(),
        duration = duration,
        currentPosition = currentPosition,
        isPlaying = playbackState == PlaybackState.PLAYING,
        hasNext = hasNext,
        hasPrevious = hasPrevious
    )
}
