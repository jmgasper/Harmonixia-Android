package com.harmonixia.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harmonixia.android.data.local.entity.CachedArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedArtistDao {
    @Query("SELECT * FROM cached_artists ORDER BY sortIndex ASC")
    fun observeCachedArtists(): Flow<List<CachedArtistEntity>>

    @Query("SELECT * FROM cached_artists ORDER BY sortIndex ASC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<CachedArtistEntity>

    @Query("SELECT COUNT(*) FROM cached_artists")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(DISTINCT syncId) FROM cached_artists")
    suspend fun getDistinctSyncIdCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(artists: List<CachedArtistEntity>)

    @Query("DELETE FROM cached_artists WHERE syncId != :syncId")
    suspend fun deleteStale(syncId: Long)
}
