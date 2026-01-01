package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.service.playback.PlaybackStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayPlaylistUseCase(
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager
) {
    suspend operator fun invoke(
        playlistId: String,
        provider: String,
        startIndex: Int = 0,
        forceStartIndex: Boolean = false,
        shuffleMode: Boolean? = null,
        tracksOverride: List<Track>? = null,
        playlistUri: String? = null,
        startItemUri: String? = null
    ): Result<String> {
        return runCatching {
            playbackStateManager.notifyUserInitiatedPlayback()
            val tracks = tracksOverride?.takeIf { it.isNotEmpty() }
            val playerId = playbackStateManager.currentPlayerId
                ?: throw IllegalStateException("No player selected")
            val queue = repository.getActiveQueue(playerId, includeItems = false).getOrThrow()
                ?: throw IllegalStateException("No active queue")
            val queueId = queue.queueId
            if (tracksOverride != null && tracksOverride.isEmpty()) {
                throw IllegalStateException("Playlist has no tracks")
            }
            val startItemIndex = if (!startItemUri.isNullOrBlank() && !tracks.isNullOrEmpty()) {
                tracks.indexOfFirst { it.uri == startItemUri }.takeIf { it >= 0 }
            } else {
                null
            }
            val safeIndex = when {
                startItemIndex != null -> startItemIndex
                tracks != null -> startIndex.coerceIn(0, tracks.lastIndex)
                else -> startIndex.coerceAtLeast(0)
            }
            if (!tracks.isNullOrEmpty()) {
                playbackStateManager.seedQueue(tracks, safeIndex)
            }
            val requestedShuffle = shuffleMode ?: queue.shuffle
            val shouldDisableShuffle = forceStartIndex && requestedShuffle
            val resolvedStartItem = startItemUri?.takeIf { it.isNotBlank() }
                ?: tracks?.getOrNull(safeIndex)?.uri?.takeIf { it.isNotBlank() }
            val resolvedPlaylistUri = playlistUri?.takeIf { it.isNotBlank() }
                ?: repository.getCachedPlaylist(playlistId, provider)?.uri?.takeIf { it.isNotBlank() }
            val playbackResult = withContext(Dispatchers.IO) {
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
                    if (!resolvedPlaylistUri.isNullOrBlank()) {
                        return@withContext repository.playMediaItem(
                            queueId = queueId,
                            media = resolvedPlaylistUri,
                            option = QueueOption.REPLACE,
                            startItem = resolvedStartItem
                        )
                    }
                    val fallbackTracks = tracks ?: repository.getPlaylistTracks(playlistId, provider).getOrThrow()
                    if (fallbackTracks.isEmpty()) {
                        return@withContext Result.failure(IllegalStateException("Playlist has no tracks"))
                    }
                    val fallbackIndex = startIndex.coerceIn(0, fallbackTracks.lastIndex)
                    if (tracks == null) {
                        playbackStateManager.seedQueue(fallbackTracks, fallbackIndex)
                    }
                    val uris = fallbackTracks.map { it.uri }
                    val playResult = repository.playMedia(queueId, uris, QueueOption.REPLACE)
                    if (playResult.isFailure) {
                        return@withContext playResult
                    }
                    if (fallbackIndex > 0) {
                        val indexResult = repository.playIndex(queueId, fallbackIndex)
                        if (indexResult.isFailure) {
                            return@withContext indexResult
                        }
                    }
                    return@withContext Result.success(Unit)
                } finally {
                    if (shuffleMode == null) {
                        if (restoreShuffle) {
                            repository.setShuffleMode(queueId, true)
                        }
                    } else if (enableShuffleAfter) {
                        repository.setShuffleMode(queueId, true)
                    }
                }
            }
            playbackResult.getOrThrow()
            playerId
        }
    }
}
