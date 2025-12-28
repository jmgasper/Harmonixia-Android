package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger

class RenamePlaylistUseCase(
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke(
        playlistId: String,
        provider: String,
        newName: String
    ): Result<Playlist> {
        val trimmed = newName.trim()
        if (playlistId.isBlank() || provider.isBlank()) {
            return Result.failure(IllegalArgumentException("Playlist details are required"))
        }
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Playlist name is required"))
        }
        return runCatching {
            val tracks = repository.getPlaylistTracks(playlistId, provider).getOrThrow()
            val newPlaylist = repository.createPlaylist(trimmed).getOrThrow()
            val uris = tracks.mapNotNull { track -> track.uri.takeIf { it.isNotBlank() } }
            if (uris.isNotEmpty()) {
                repository.addTracksToPlaylist(newPlaylist.itemId, uris).getOrThrow()
            }
            val deleteResult = repository.deletePlaylist(playlistId)
            if (deleteResult.isFailure) {
                Logger.w(TAG, "Failed to delete playlist after rename", deleteResult.exceptionOrNull())
            }
            newPlaylist
        }
    }

    private companion object {
        private const val TAG = "RenamePlaylistUseCase"
    }
}
