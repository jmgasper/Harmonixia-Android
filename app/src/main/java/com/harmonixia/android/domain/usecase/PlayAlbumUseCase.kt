package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.service.playback.PlaybackStateManager
import com.harmonixia.android.util.PlayerSelection

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
            val player = selectPlayer(repository.fetchPlayers().getOrThrow())
            val queue = repository.getActiveQueue(player.playerId).getOrThrow()
                ?: throw IllegalStateException("No active queue")
            val queueId = queue.queueId
            val uris = tracks.map { it.uri }
            val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
            val requestedShuffle = shuffleMode ?: queue.shuffle
            val shouldDisableShuffle = forceStartIndex && requestedShuffle
            var restoreShuffle = false
            var enableShuffleAfter = false
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
            } finally {
                if (shuffleMode == null) {
                    if (restoreShuffle) {
                        repository.setShuffleMode(queueId, true)
                    }
                } else if (enableShuffleAfter) {
                    repository.setShuffleMode(queueId, true)
                }
            }
            player.playerId
        }
    }

    private fun selectPlayer(players: List<Player>): Player {
        return PlayerSelection.selectLocalPlayer(players)
            ?: throw IllegalStateException("No local playback device available")
    }
}
