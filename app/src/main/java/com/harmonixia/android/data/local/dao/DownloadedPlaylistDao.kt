package com.harmonixia.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.harmonixia.android.data.local.entity.DownloadedPlaylistEntity
import com.harmonixia.android.data.local.entity.DownloadedPlaylistWithTracks
import com.harmonixia.android.data.local.entity.PlaylistTrackCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedPlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: DownloadedPlaylistEntity)

    @Update
    suspend fun update(playlist: DownloadedPlaylistEntity)

    @Delete
    suspend fun delete(playlist: DownloadedPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackRefs(refs: List<PlaylistTrackCrossRef>)

    @Query("SELECT trackId FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun getTrackIdsForPlaylist(playlistId: String): List<String>

    @Query(
        "SELECT DISTINCT trackId FROM playlist_track_cross_ref " +
            "WHERE trackId IN (:trackIds)"
    )
    suspend fun getTrackIdsWithPlaylistRefs(trackIds: List<String>): List<String>

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun deleteTrackRefsForPlaylist(playlistId: String)

    @Query("DELETE FROM playlist_track_cross_ref WHERE trackId = :trackId")
    suspend fun deleteTrackRefsForTrack(trackId: String)

    @Query("SELECT * FROM downloaded_playlists")
    fun getPlaylists(): Flow<List<DownloadedPlaylistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_playlists WHERE downloadId = :downloadId)")
    suspend fun exists(downloadId: String): Boolean

    @Query("SELECT * FROM downloaded_playlists WHERE downloadId = :downloadId")
    suspend fun getPlaylistOnce(downloadId: String): DownloadedPlaylistEntity?

    @Query("DELETE FROM downloaded_playlists WHERE downloadId = :downloadId")
    suspend fun deleteById(downloadId: String)

    @Transaction
    @Query("SELECT * FROM downloaded_playlists WHERE downloadId = :downloadId")
    fun getPlaylistWithTracks(downloadId: String): Flow<DownloadedPlaylistWithTracks?>
}
