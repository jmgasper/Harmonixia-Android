package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadLibraryUseCaseTest {

    private val repository = mockk<MusicAssistantRepository>()
    private val useCase = LoadLibraryUseCase(repository)

    @Test
    fun invoke_success_returnsLibraryData() = runBlocking {
        val albums = listOf(
            Album(
                itemId = "album-1",
                provider = "test",
                uri = "test://album-1",
                name = "Album One"
            )
        )
        val artists = listOf(
            Artist(
                itemId = "artist-1",
                provider = "test",
                uri = "test://artist-1",
                name = "Artist One"
            )
        )
        val playlists = listOf(
            Playlist(
                itemId = "playlist-1",
                provider = "test",
                uri = "test://playlist-1",
                name = "Playlist One"
            )
        )

        coEvery { repository.fetchAlbums(200, 0) } returns Result.success(albums)
        coEvery { repository.fetchArtists(200, 0) } returns Result.success(artists)
        coEvery { repository.fetchPlaylists(200, 0) } returns Result.success(playlists)

        val result = useCase()

        assertTrue(result.isSuccess)
        val library = result.getOrThrow()
        assertEquals(albums, library.albums)
        assertEquals(artists, library.artists)
        assertEquals(playlists, library.playlists)
    }

    @Test
    fun invoke_failure_propagatesErrors() = runBlocking {
        coEvery { repository.fetchAlbums(200, 0) } returns Result.failure(
            IllegalStateException("Failure")
        )
        coEvery { repository.fetchArtists(200, 0) } returns Result.success(emptyList())
        coEvery { repository.fetchPlaylists(200, 0) } returns Result.success(emptyList())

        val result = useCase()

        assertTrue(result.isFailure)
    }
}
