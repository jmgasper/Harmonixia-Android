package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.repository.MusicAssistantRepository

class DeletePlaylistUseCase(
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke(playlistId: String): Result<Unit> {
        if (playlistId.isBlank()) {
            return Result.failure(IllegalArgumentException("Playlist id is required"))
        }
        return repository.deletePlaylist(playlistId)
    }
}
