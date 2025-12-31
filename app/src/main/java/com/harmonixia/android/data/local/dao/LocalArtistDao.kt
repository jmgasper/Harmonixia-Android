package com.harmonixia.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harmonixia.android.data.local.entity.LocalArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalArtistDao {
    @Query("SELECT * FROM local_artists ORDER BY name")
    fun getAllArtists(): Flow<List<LocalArtistEntity>>

    @Query("SELECT * FROM local_artists WHERE name LIKE :query")
    fun searchArtists(query: String): Flow<List<LocalArtistEntity>>

    @Query("SELECT * FROM local_artists WHERE name = :name")
    fun getArtistByName(name: String): Flow<LocalArtistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtist(artist: LocalArtistEntity)

    @Query("DELETE FROM local_artists")
    suspend fun deleteAllArtists()
}
