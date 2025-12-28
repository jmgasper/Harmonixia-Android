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
        startIndex: Int = 0
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
            if (safeIndex > 0) {
                repository.clearQueue(queueId).getOrThrow()
                repository.playMedia(queueId, uris, QueueOption.ADD).getOrThrow()
                repository.playIndex(queueId, safeIndex).getOrThrow()
            } else {
                repository.playMedia(queueId, uris, QueueOption.REPLACE).getOrThrow()
            }
            player.playerId
        }
    }

    private fun selectPlayer(players: List<Player>): Player {
        return PlayerSelection.selectLocalPlayer(players)
            ?: throw IllegalStateException("No local playback device available")
    }
}
