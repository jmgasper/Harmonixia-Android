package com.harmonixia.android.data.repository

import android.net.Uri
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import com.harmonixia.android.domain.repository.OfflineLibraryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class OfflineLibraryRepositoryImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : OfflineLibraryRepository {
    override fun getDownloadedAlbumsByArtist(artistName: String): Flow<List<Album>> {
        val normalized = normalizeName(artistName)
        if (normalized.isBlank()) return flowOf(emptyList())
        return downloadRepository.getDownloadedAlbums().map { albums ->
            albums.filter { album ->
                album.artists.any { name -> normalizeName(name) == normalized }
            }
        }
    }

    override fun getDownloadedTracksByArtist(artistName: String): Flow<List<Track>> {
        val normalized = normalizeName(artistName)
        if (normalized.isBlank()) return flowOf(emptyList())
        return downloadRepository.getDownloadedTracks().map { tracks ->
            tracks.filter { track ->
                normalizeName(track.artist) == normalized
            }
        }
    }

    override fun searchDownloadedContent(query: String): Flow<SearchResults> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return flowOf(SearchResults())
        val normalizedQuery = trimmed.lowercase()
        return combine(
            downloadRepository.getDownloadedAlbums(),
            downloadRepository.getDownloadedPlaylists(),
            downloadRepository.getDownloadedTracks()
        ) { albums, playlists, tracks ->
            val filteredAlbums = albums.filter { album -> matchesAlbum(album, normalizedQuery) }
            val filteredPlaylists = playlists.filter { playlist -> matchesPlaylist(playlist, normalizedQuery) }
            val filteredTracks = tracks.filter { track -> matchesTrack(track, normalizedQuery) }
            val filteredArtists = buildOfflineArtists(albums, tracks, normalizedQuery)
            SearchResults(
                albums = filteredAlbums,
                artists = filteredArtists,
                playlists = filteredPlaylists,
                tracks = filteredTracks
            )
        }
    }

    private fun matchesAlbum(album: Album, query: String): Boolean {
        return album.name.contains(query, ignoreCase = true) ||
            album.artists.any { name -> name.contains(query, ignoreCase = true) }
    }

    private fun matchesPlaylist(playlist: Playlist, query: String): Boolean {
        return playlist.name.contains(query, ignoreCase = true) ||
            (playlist.owner?.contains(query, ignoreCase = true) == true)
    }

    private fun matchesTrack(track: Track, query: String): Boolean {
        return track.title.contains(query, ignoreCase = true) ||
            track.artist.contains(query, ignoreCase = true) ||
            track.album.contains(query, ignoreCase = true)
    }

    private fun buildOfflineArtists(
        albums: List<Album>,
        tracks: List<Track>,
        query: String
    ): List<Artist> {
        val imageByName = mutableMapOf<String, String?>()
        for (album in albums) {
            for (artist in album.artists) {
                val normalized = normalizeName(artist)
                if (normalized.isBlank()) continue
                if (!imageByName.containsKey(normalized)) {
                    imageByName[normalized] = album.imageUrl
                }
            }
        }
        for (track in tracks) {
            val normalized = normalizeName(track.artist)
            if (normalized.isBlank()) continue
            if (!imageByName.containsKey(normalized)) {
                imageByName[normalized] = track.imageUrl
            }
        }
        val candidates = imageByName.keys
            .mapNotNull { key ->
                val name = albums.asSequence()
                    .flatMap { it.artists.asSequence() }
                    .firstOrNull { normalizeName(it) == key }
                    ?: tracks.firstOrNull { normalizeName(it.artist) == key }?.artist
                name
            }
            .distinct()
            .filter { name -> name.contains(query, ignoreCase = true) }
            .sortedBy { it.lowercase() }
        return candidates.map { name ->
            val normalized = normalizeName(name)
            createOfflineArtist(name, imageByName[normalized])
        }
    }

    private fun createOfflineArtist(name: String, imageUrl: String?): Artist {
        val trimmed = name.trim()
        val encodedId = Uri.encode(trimmed)
        return Artist(
            itemId = encodedId,
            provider = OFFLINE_PROVIDER,
            uri = "offline:artist:$encodedId",
            name = trimmed,
            sortName = trimmed.lowercase(),
            imageUrl = imageUrl
        )
    }

    private fun normalizeName(name: String?): String {
        return name?.trim()?.lowercase().orEmpty()
    }
}
