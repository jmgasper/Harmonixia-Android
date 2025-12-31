package com.harmonixia.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harmonixia.android.data.local.entity.LocalAlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalAlbumDao {
    @Query("SELECT * FROM local_albums ORDER BY artist, name")
    fun getAllAlbums(): Flow<List<LocalAlbumEntity>>

    @Query("SELECT * FROM local_albums WHERE artist = :artist ORDER BY name")
    fun getAlbumsByArtist(artist: String): Flow<List<LocalAlbumEntity>>

    @Query("SELECT * FROM local_albums WHERE name LIKE :query OR artist LIKE :query")
    fun searchAlbums(query: String): Flow<List<LocalAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbum(album: LocalAlbumEntity)

    @Query("DELETE FROM local_albums")
    suspend fun deleteAllAlbums()
}
