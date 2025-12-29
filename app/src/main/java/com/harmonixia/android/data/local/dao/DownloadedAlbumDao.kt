package com.harmonixia.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.harmonixia.android.data.local.entity.DownloadedAlbumEntity
import com.harmonixia.android.data.local.entity.DownloadedAlbumWithTracks
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedAlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: DownloadedAlbumEntity)

    @Update
    suspend fun update(album: DownloadedAlbumEntity)

    @Delete
    suspend fun delete(album: DownloadedAlbumEntity)

    @Query("SELECT * FROM downloaded_albums")
    fun getAlbums(): Flow<List<DownloadedAlbumEntity>>

    @Transaction
    @Query("SELECT * FROM downloaded_albums")
    fun getAlbumsWithTracks(): Flow<List<DownloadedAlbumWithTracks>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_albums WHERE downloadId = :downloadId)")
    suspend fun exists(downloadId: String): Boolean

    @Query("SELECT * FROM downloaded_albums WHERE downloadId = :downloadId")
    suspend fun getAlbumOnce(downloadId: String): DownloadedAlbumEntity?

    @Query("DELETE FROM downloaded_albums WHERE downloadId = :downloadId")
    suspend fun deleteById(downloadId: String)

    @Transaction
    @Query("SELECT * FROM downloaded_albums WHERE downloadId = :downloadId")
    fun getAlbumWithTracks(downloadId: String): Flow<DownloadedAlbumWithTracks?>
}
