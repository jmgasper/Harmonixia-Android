package com.harmonixia.android.domain.usecase

import android.content.Context
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.service.playback.PlaybackStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayTrackUseCase(
    private val context: Context,
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager
) {
    suspend operator fun invoke(track: Track): Result<String> {
        return runCatching {
            playbackStateManager.notifyUserInitiatedPlayback()
            playbackStateManager.reconnectLocalPlayerIfUnavailable()
            val playerId = playbackStateManager.currentPlayerId
                ?: throw IllegalStateException(context.getString(R.string.playback_error_no_player_selected))
            val queue = repository.getActiveQueue(playerId, includeItems = false).getOrThrow()
                ?: throw IllegalStateException(context.getString(R.string.playback_error_no_active_queue))
            val queueId = queue.queueId
            val uri = track.uri.trim()
            if (uri.isBlank()) {
                throw IllegalArgumentException(context.getString(R.string.playback_error_track_uri_required))
            }
            playbackStateManager.clearPendingStart()
            withContext(Dispatchers.IO) {
                repository.playMedia(queueId, listOf(uri), QueueOption.REPLACE)
            }.getOrThrow()
            playbackStateManager.refreshQueueFast()
            playerId
        }
    }
}
