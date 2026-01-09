package com.harmonixia.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harmonixia.android.data.local.entity.CachedAlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedAlbumDao {
    @Query("SELECT * FROM cached_albums ORDER BY sortIndex ASC")
    fun observeCachedAlbums(): Flow<List<CachedAlbumEntity>>

    @Query("SELECT * FROM cached_albums ORDER BY sortIndex ASC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<CachedAlbumEntity>

    @Query("SELECT COUNT(*) FROM cached_albums")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(DISTINCT syncId) FROM cached_albums")
    suspend fun getDistinctSyncIdCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(albums: List<CachedAlbumEntity>)

    @Query("DELETE FROM cached_albums WHERE syncId != :syncId")
    suspend fun deleteStale(syncId: Long)
}
