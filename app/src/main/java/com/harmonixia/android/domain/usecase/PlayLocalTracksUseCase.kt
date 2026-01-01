package com.harmonixia.android.domain.usecase

import androidx.media3.common.util.UnstableApi
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.service.playback.PlaybackServiceConnection
import com.harmonixia.android.util.toPlaybackMediaItem
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(UnstableApi::class)
class PlayLocalTracksUseCase(
    private val playbackServiceConnection: PlaybackServiceConnection
) {
    suspend operator fun invoke(
        tracks: List<Track>,
        startIndex: Int = 0
    ): Result<Unit> {
        if (tracks.isEmpty()) {
            return Result.failure(IllegalArgumentException("No tracks to play"))
        }
        playbackServiceConnection.connect()
        val controller = withTimeoutOrNull(CONTROLLER_TIMEOUT_MS) {
            playbackServiceConnection.mediaController
                .filterNotNull()
                .first()
        } ?: return Result.failure(IllegalStateException("Playback service unavailable"))
        val mediaItems = tracks.map { it.toPlaybackMediaItem() }
        val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex)
        controller.setMediaItems(mediaItems, safeIndex, 0L)
        controller.prepare()
        controller.play()
        return Result.success(Unit)
    }

    private companion object {
        private const val CONTROLLER_TIMEOUT_MS = 3_000L
    }
}
