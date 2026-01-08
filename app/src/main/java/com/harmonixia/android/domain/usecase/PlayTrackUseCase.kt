package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.service.playback.PlaybackStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayTrackUseCase(
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager
) {
    suspend operator fun invoke(track: Track): Result<String> {
        return runCatching {
            playbackStateManager.notifyUserInitiatedPlayback()
            val playerId = playbackStateManager.currentPlayerId
                ?: throw IllegalStateException("No player selected")
            val queue = repository.getActiveQueue(playerId, includeItems = false).getOrThrow()
                ?: throw IllegalStateException("No active queue")
            val queueId = queue.queueId
            val uri = track.uri.trim()
            if (uri.isBlank()) {
                throw IllegalArgumentException("Track URI is required")
            }
            playbackStateManager.seedQueue(listOf(track), 0)
            withContext(Dispatchers.IO) {
                repository.playMedia(queueId, listOf(uri), QueueOption.REPLACE)
            }.getOrThrow()
            playerId
        }
    }
}
