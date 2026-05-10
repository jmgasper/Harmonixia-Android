package com.harmonixia.android.domain.usecase

import android.content.Context
import com.harmonixia.android.R
import com.harmonixia.android.domain.repository.MusicAssistantRepository

class DeletePlaylistUseCase(
    private val context: Context,
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke(playlistId: String): Result<Unit> {
        if (playlistId.isBlank()) {
            return Result.failure(
                IllegalArgumentException(context.getString(R.string.playlist_validation_id_required))
            )
        }
        return repository.deletePlaylist(playlistId)
    }
}
