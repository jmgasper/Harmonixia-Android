package com.harmonixia.android.domain.usecase

import android.content.Context
import com.harmonixia.android.R
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ManagePlaylistTracksUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
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
            return Result.failure(
                IllegalStateException(context.getString(R.string.playlist_validation_not_editable))
            )
        }
        if (playlistId.isBlank()) {
            return Result.failure(
                IllegalArgumentException(context.getString(R.string.playlist_validation_id_required))
            )
        }
        if (trackUris.isEmpty()) {
            return Result.success(Unit)
        }
        if (trackUris.any { it.isBlank() }) {
            return Result.failure(
                IllegalArgumentException(
                    context.getString(R.string.playlist_validation_track_uri_required)
                )
            )
        }
        return repository.addTracksToPlaylist(playlistId, trackUris)
    }

    suspend fun removeTracksFromPlaylist(
        playlistId: String,
        positions: List<Int>
    ): Result<Unit> {
        if (playlistId.isBlank()) {
            return Result.failure(
                IllegalArgumentException(context.getString(R.string.playlist_validation_id_required))
            )
        }
        if (positions.isEmpty()) {
            return Result.success(Unit)
        }
        if (positions.any { it < 0 }) {
            return Result.failure(
                IllegalArgumentException(
                    context.getString(R.string.playlist_validation_track_position_required)
                )
            )
        }
        return repository.removeTracksFromPlaylist(playlistId, positions)
    }
}
