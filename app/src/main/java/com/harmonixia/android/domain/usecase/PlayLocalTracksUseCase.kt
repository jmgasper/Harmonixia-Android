package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.service.playback.PlaybackStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayLocalTracksUseCase(
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager
) {
    suspend operator fun invoke(
        tracks: List<Track>,
        startIndex: Int = 0,
        shuffleMode: Boolean? = null
    ): Result<Unit> {
        if (tracks.isEmpty()) {
            return Result.failure(IllegalArgumentException("No tracks to play"))
        }
        playbackStateManager.notifyUserInitiatedPlayback()
        playbackStateManager.reconnectLocalPlayerIfUnavailable()
        val playerId = playbackStateManager.currentPlayerId
            ?: return Result.failure(IllegalStateException("No player selected"))
        val queue = repository.getActiveQueue(playerId, includeItems = false).getOrThrow()
            ?: return Result.failure(IllegalStateException("No active queue"))
        val queueId = queue.queueId
        val uris = tracks.map { it.uri.trim() }
        if (uris.any { it.isBlank() }) {
            return Result.failure(IllegalArgumentException("Track URI is required"))
        }
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        if (safeIndex > 0) {
            playbackStateManager.registerPendingStart(tracks[safeIndex].itemId)
        } else {
            playbackStateManager.clearPendingStart()
        }
        val playResult = withContext(Dispatchers.IO) {
            val playResult = repository.playMedia(queueId, uris, QueueOption.REPLACE)
            if (playResult.isFailure) return@withContext playResult
            if (safeIndex > 0) {
                val indexResult = repository.playIndex(queueId, safeIndex)
                if (indexResult.isFailure) return@withContext indexResult
            }
            if (shuffleMode != null && queue.shuffle != shuffleMode) {
                repository.setShuffleMode(queueId, shuffleMode)
            }
            Result.success(Unit)
        }
        if (playResult.isFailure) {
            return playResult
        }
        playbackStateManager.refreshQueueFast()
        return Result.success(Unit)
    }
}
