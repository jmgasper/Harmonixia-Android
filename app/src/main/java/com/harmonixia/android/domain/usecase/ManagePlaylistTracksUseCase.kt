package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject

class ManagePlaylistTracksUseCase @Inject constructor(
    private val repository: MusicAssistantRepository
) {
    suspend fun addTrackToPlaylist(
        playlistId: String,
        trackUri: String,
        isEditable: Boolean = true
    ): Result<Unit> {
        if (!isEditable) {
            return Result.failure(IllegalStateException("Playlist is not editable"))
        }
        if (playlistId.isBlank()) {
            return Result.failure(IllegalArgumentException("Playlist id is required"))
        }
        if (trackUri.isBlank()) {
            return Result.failure(IllegalArgumentException("Track uri is required"))
        }
        return repository.addTracksToPlaylist(playlistId, listOf(trackUri))
    }

    suspend fun removeTrackFromPlaylist(
        playlistId: String,
        position: Int
    ): Result<Unit> {
        if (position < 0) {
            return Result.failure(IllegalArgumentException("Track position is required"))
        }
        return repository.removeTracksFromPlaylist(playlistId, listOf(position))
    }
}
