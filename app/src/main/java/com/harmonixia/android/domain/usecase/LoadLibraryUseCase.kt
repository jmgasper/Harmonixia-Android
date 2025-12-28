package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.LibraryData
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class LoadLibraryUseCase @Inject constructor(
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke(): Result<LibraryData> = coroutineScope {
        runCatching {
            val albumsDeferred = async { repository.fetchAlbums(DEFAULT_PAGE_SIZE, 0) }
            val artistsDeferred = async { repository.fetchArtists(DEFAULT_PAGE_SIZE, 0) }
            val playlistsDeferred = async { repository.fetchPlaylists(DEFAULT_PAGE_SIZE, 0) }

            val albumsResult = albumsDeferred.await()
            val artistsResult = artistsDeferred.await()
            val playlistsResult = playlistsDeferred.await()

            val error = albumsResult.exceptionOrNull()
                ?: artistsResult.exceptionOrNull()
                ?: playlistsResult.exceptionOrNull()
            if (error != null) {
                throw error
            }

            LibraryData(
                albums = albumsResult.getOrDefault(emptyList()),
                artists = artistsResult.getOrDefault(emptyList()),
                playlists = playlistsResult.getOrDefault(emptyList())
            )
        }
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 200
    }
}
