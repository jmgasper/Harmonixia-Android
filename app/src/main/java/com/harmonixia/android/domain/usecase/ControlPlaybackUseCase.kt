package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.service.playback.PlaybackStateManager
import com.harmonixia.android.util.PlayerSelection

enum class PlaybackCommand {
    PLAY,
    PAUSE,
    NEXT,
    PREVIOUS,
    SEEK
}

class ControlPlaybackUseCase(
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager
) {
    suspend operator fun invoke(command: PlaybackCommand, position: Int? = null): Result<Unit> {
        return runCatching {
            val player = selectPlayer(repository.fetchPlayers().getOrThrow())
            val queue = repository.getActiveQueue(player.playerId).getOrThrow()
                ?: throw IllegalStateException("No active queue")
            val queueId = queue.queueId
            when (command) {
                PlaybackCommand.PLAY -> {
                    playbackStateManager.notifyUserInitiatedPlayback()
                    repository.resumeQueue(queueId).getOrThrow()
                }
                PlaybackCommand.PAUSE -> repository.pauseQueue(queueId).getOrThrow()
                PlaybackCommand.NEXT -> repository.nextTrack(queueId).getOrThrow()
                PlaybackCommand.PREVIOUS -> repository.previousTrack(queueId).getOrThrow()
                PlaybackCommand.SEEK -> {
                    val seekPosition = position
                        ?: throw IllegalArgumentException("Position required for SEEK")
                    repository.seekTo(queueId, seekPosition).getOrThrow()
                }
            }
        }
    }

    private fun selectPlayer(players: List<Player>): Player {
        return PlayerSelection.selectLocalPlayer(players)
            ?: throw IllegalStateException("No local playback device available")
    }
}
