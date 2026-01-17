package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.service.playback.PlaybackStateManager

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
            val playerId = playbackStateManager.currentPlayerId
                ?: throw IllegalStateException("No player selected")
            val queue = repository.getActiveQueue(playerId, includeItems = false).getOrThrow()
                ?: throw IllegalStateException("No active queue")
            val queueId = queue.queueId
            when (command) {
                PlaybackCommand.PLAY -> {
                    playbackStateManager.notifyUserInitiatedPlayback()
                    repository.resumeQueue(queueId).getOrThrow()
                }
                PlaybackCommand.PAUSE -> {
                    playbackStateManager.notifyUserInitiatedPause()
                    repository.pauseQueue(queueId).getOrThrow()
                }
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
}
