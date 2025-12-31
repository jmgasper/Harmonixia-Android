package com.harmonixia.android.data.repository

import com.harmonixia.android.data.local.dao.LocalAlbumDao
import com.harmonixia.android.data.local.dao.LocalArtistDao
import com.harmonixia.android.data.local.dao.LocalTrackDao
import com.harmonixia.android.data.local.entity.toAlbum
import com.harmonixia.android.data.local.entity.toArtist
import com.harmonixia.android.data.local.entity.toTrack
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.LocalMediaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class LocalMediaRepositoryImpl @Inject constructor(
    private val localTrackDao: LocalTrackDao,
    private val localAlbumDao: LocalAlbumDao,
    private val localArtistDao: LocalArtistDao
) : LocalMediaRepository {
    override fun getAllTracks(): Flow<List<Track>> {
        return localTrackDao.getAllTracks().map { tracks -> tracks.map { it.toTrack() } }
    }

    override fun getTracksByArtist(artistName: String): Flow<List<Track>> {
        val trimmed = artistName.trim()
        if (trimmed.isBlank()) return flowOf(emptyList())
        return localTrackDao.getTracksByArtist(trimmed).map { tracks -> tracks.map { it.toTrack() } }
    }

    override fun getTracksByAlbum(
        albumName: String,
        albumArtistName: String
    ): Flow<List<Track>> {
        val trimmedAlbum = albumName.trim()
        val trimmedAlbumArtist = albumArtistName.trim()
        if (trimmedAlbum.isBlank() || trimmedAlbumArtist.isBlank()) return flowOf(emptyList())
        return localTrackDao.getTracksByAlbum(trimmedAlbum, trimmedAlbumArtist)
            .map { tracks -> tracks.map { it.toTrack() } }
    }

    override fun getAllAlbums(): Flow<List<Album>> {
        return localAlbumDao.getAllAlbums().map { albums -> albums.map { it.toAlbum() } }
    }

    override fun getAlbumsByArtist(artistName: String): Flow<List<Album>> {
        val trimmed = artistName.trim()
        if (trimmed.isBlank()) return flowOf(emptyList())
        return localAlbumDao.getAlbumsByArtist(trimmed).map { albums -> albums.map { it.toAlbum() } }
    }

    override fun getAllArtists(): Flow<List<Artist>> {
        return localArtistDao.getAllArtists().map { artists -> artists.map { it.toArtist() } }
    }

    override fun searchTracks(query: String): Flow<List<Track>> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return flowOf(emptyList())
        val sqlQuery = "%$trimmed%"
        return localTrackDao.searchTracks(sqlQuery).map { tracks -> tracks.map { it.toTrack() } }
    }

    override fun searchAlbums(query: String): Flow<List<Album>> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return flowOf(emptyList())
        val sqlQuery = "%$trimmed%"
        return localAlbumDao.searchAlbums(sqlQuery).map { albums -> albums.map { it.toAlbum() } }
    }

    override fun searchArtists(query: String): Flow<List<Artist>> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return flowOf(emptyList())
        val sqlQuery = "%$trimmed%"
        return localArtistDao.searchArtists(sqlQuery).map { artists -> artists.map { it.toArtist() } }
    }

    override fun getTrackByFilePath(filePath: String): Flow<Track?> {
        val trimmed = filePath.trim()
        if (trimmed.isBlank()) return flowOf(null)
        return localTrackDao.getTrackByFilePath(trimmed).map { track -> track?.toTrack() }
    }

    override suspend fun clearAllLocalMedia() {
        localTrackDao.deleteAllTracks()
        localAlbumDao.deleteAllAlbums()
        localArtistDao.deleteAllArtists()
    }
}
