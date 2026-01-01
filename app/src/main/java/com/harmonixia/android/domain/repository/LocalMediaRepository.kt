package com.harmonixia.android.domain.repository

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface LocalMediaRepository {
    fun getAllTracks(): Flow<List<Track>>

    fun getTracksByArtist(artistName: String): Flow<List<Track>>

    fun getTracksByAlbum(albumName: String, albumArtistName: String): Flow<List<Track>>

    fun getAllAlbums(): Flow<List<Album>>

    fun getAlbumsByArtist(artistName: String): Flow<List<Album>>

    fun getAlbumByNameAndArtist(albumName: String, artistName: String): Flow<Album?>

    fun getAllArtists(): Flow<List<Artist>>

    fun searchTracks(query: String): Flow<List<Track>>

    fun searchAlbums(query: String): Flow<List<Album>>

    fun searchArtists(query: String): Flow<List<Artist>>

    fun getTrackByFilePath(filePath: String): Flow<Track?>

    suspend fun clearAllLocalMedia()
}
