package com.harmonixia.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harmonixia.android.data.local.entity.LocalTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalTrackDao {
    @Query("SELECT * FROM local_tracks ORDER BY artist, album, trackNumber")
    fun getAllTracks(): Flow<List<LocalTrackEntity>>

    @Query("SELECT * FROM local_tracks WHERE artist = :artist ORDER BY album, trackNumber")
    fun getTracksByArtist(artist: String): Flow<List<LocalTrackEntity>>

    @Query(
        "SELECT * FROM local_tracks " +
            "WHERE album = :album AND (" +
            "albumArtist = :albumArtist OR " +
            "((albumArtist IS NULL OR albumArtist = '') AND artist = :albumArtist)" +
            ") ORDER BY trackNumber"
    )
    fun getTracksByAlbum(album: String, albumArtist: String): Flow<List<LocalTrackEntity>>

    @Query(
        "SELECT * FROM local_tracks WHERE title LIKE :query OR artist LIKE :query " +
            "OR album LIKE :query"
    )
    fun searchTracks(query: String): Flow<List<LocalTrackEntity>>

    @Query("SELECT * FROM local_tracks WHERE filePath = :filePath")
    fun getTrackByFilePath(filePath: String): Flow<LocalTrackEntity?>

    @Query("SELECT COUNT(*) FROM local_tracks")
    fun getTrackCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM local_tracks WHERE filePath = :filePath")
    fun getTrackCountByFilePath(filePath: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: LocalTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<LocalTrackEntity>)

    @Delete
    suspend fun deleteTrack(track: LocalTrackEntity)

    @Query("DELETE FROM local_tracks")
    suspend fun deleteAllTracks()
}
