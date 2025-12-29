package com.harmonixia.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.harmonixia.android.data.local.entity.DownloadedTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: DownloadedTrackEntity)

    @Update
    suspend fun update(track: DownloadedTrackEntity)

    @Delete
    suspend fun delete(track: DownloadedTrackEntity)

    @Query("SELECT * FROM downloaded_tracks WHERE downloadId = :downloadId")
    fun getTrack(downloadId: String): Flow<DownloadedTrackEntity?>

    @Query("SELECT * FROM downloaded_tracks WHERE downloadStatus = :status")
    fun getTracksByStatus(status: String): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks WHERE albumId = :albumDownloadId")
    fun getTracksByAlbum(albumDownloadId: String): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks WHERE itemId = :itemId")
    fun getTracksByItemId(itemId: String): Flow<List<DownloadedTrackEntity>>

    @Query(
        "SELECT * FROM downloaded_tracks " +
            "WHERE downloadId IN (:downloadIds) AND albumId IS NULL"
    )
    suspend fun getTracksWithoutAlbum(downloadIds: List<String>): List<DownloadedTrackEntity>

    @Query("SELECT * FROM downloaded_tracks")
    suspend fun getAllOnce(): List<DownloadedTrackEntity>

    @Query("DELETE FROM downloaded_tracks WHERE albumId = :albumDownloadId")
    suspend fun deleteByAlbum(albumDownloadId: String)

    @Query("DELETE FROM downloaded_tracks")
    suspend fun deleteAll()
}
