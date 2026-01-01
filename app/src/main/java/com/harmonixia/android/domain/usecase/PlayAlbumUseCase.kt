package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.service.playback.PlaybackStateManager

class PlayAlbumUseCase(
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager
) {
    suspend operator fun invoke(
        albumId: String,
        provider: String,
        startIndex: Int = 0,
        forceStartIndex: Boolean = false,
        shuffleMode: Boolean? = null
    ): Result<String> {
        return runCatching {
            playbackStateManager.notifyUserInitiatedPlayback()
            val tracks = repository.getAlbumTracks(albumId, provider).getOrThrow()
            if (tracks.isEmpty()) {
                throw IllegalStateException("Album has no tracks")
            }
            val playerId = playbackStateManager.currentPlayerId
                ?: throw IllegalStateException("No player selected")
            val queue = repository.getActiveQueue(playerId, includeItems = false).getOrThrow()
                ?: throw IllegalStateException("No active queue")
            val queueId = queue.queueId
            val uris = tracks.map { it.uri }
            val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
            playbackStateManager.seedQueue(tracks, safeIndex)
            val requestedShuffle = shuffleMode ?: queue.shuffle
            val shouldDisableShuffle = forceStartIndex && requestedShuffle
            var restoreShuffle = false
            var enableShuffleAfter = false
            var queueUpdated = false
            if (shuffleMode == null) {
                if (shouldDisableShuffle && queue.shuffle) {
                    restoreShuffle = repository.setShuffleMode(queueId, false).isSuccess
                }
            } else {
                if (shouldDisableShuffle) {
                    if (queue.shuffle) {
                        repository.setShuffleMode(queueId, false)
                    }
                    enableShuffleAfter = true
                } else if (queue.shuffle != requestedShuffle) {
                    repository.setShuffleMode(queueId, requestedShuffle)
                }
            }
            try {
                repository.playMedia(queueId, uris, QueueOption.REPLACE).getOrThrow()
                if (safeIndex > 0) {
                    repository.playIndex(queueId, safeIndex).getOrThrow()
                }
                queueUpdated = true
            } finally {
                if (shuffleMode == null) {
                    if (restoreShuffle) {
                        repository.setShuffleMode(queueId, true)
                    }
                } else if (enableShuffleAfter) {
                    repository.setShuffleMode(queueId, true)
                }
            }
            if (queueUpdated) {
                playbackStateManager.refreshQueueNow()
            }
            playerId
        }
    }
}
