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
        return addTracksToPlaylist(playlistId, listOf(trackUri), isEditable)
    }

    suspend fun removeTrackFromPlaylist(
        playlistId: String,
        position: Int
    ): Result<Unit> {
        return removeTracksFromPlaylist(playlistId, listOf(position))
    }

    suspend fun addTracksToPlaylist(
        playlistId: String,
        trackUris: List<String>,
        isEditable: Boolean = true
    ): Result<Unit> {
        if (!isEditable) {
            return Result.failure(IllegalStateException("Playlist is not editable"))
        }
        if (playlistId.isBlank()) {
            return Result.failure(IllegalArgumentException("Playlist id is required"))
        }
        if (trackUris.isEmpty()) {
            return Result.success(Unit)
        }
        if (trackUris.any { it.isBlank() }) {
            return Result.failure(IllegalArgumentException("Track uri is required"))
        }
        return repository.addTracksToPlaylist(playlistId, trackUris)
    }

    suspend fun removeTracksFromPlaylist(
        playlistId: String,
        positions: List<Int>
    ): Result<Unit> {
        if (playlistId.isBlank()) {
            return Result.failure(IllegalArgumentException("Playlist id is required"))
        }
        if (positions.isEmpty()) {
            return Result.success(Unit)
        }
        if (positions.any { it < 0 }) {
            return Result.failure(IllegalArgumentException("Track position is required"))
        }
        return repository.removeTracksFromPlaylist(playlistId, positions)
    }
}
