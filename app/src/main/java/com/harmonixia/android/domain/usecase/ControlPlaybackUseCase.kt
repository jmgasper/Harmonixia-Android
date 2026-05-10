package com.harmonixia.android.domain.usecase

import android.content.Context
import com.harmonixia.android.R
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
    private val context: Context,
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager
) {
    suspend operator fun invoke(command: PlaybackCommand, position: Int? = null): Result<Unit> {
        return runCatching {
            val playerId = playbackStateManager.currentPlayerId
                ?: throw IllegalStateException(context.getString(R.string.playback_error_no_player_selected))
            val queue = repository.getActiveQueue(playerId, includeItems = false).getOrThrow()
                ?: throw IllegalStateException(context.getString(R.string.playback_error_no_active_queue))
            val queueId = queue.queueId
            when (command) {
                PlaybackCommand.PLAY -> {
                    playbackStateManager.notifyUserInitiatedPlayback()
                    playbackStateManager.reconnectLocalPlayerIfUnavailable()
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
                        ?: throw IllegalArgumentException(
                            context.getString(R.string.playback_error_seek_position_required)
                        )
                    repository.seekTo(queueId, seekPosition).getOrThrow()
                }
            }
        }
    }
}
