package com.harmonixia.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.harmonixia.android.data.local.dao.LocalAlbumDao
import com.harmonixia.android.data.local.dao.LocalArtistDao
import com.harmonixia.android.data.local.dao.LocalTrackDao
import com.harmonixia.android.data.local.entity.LocalAlbumEntity
import com.harmonixia.android.data.local.entity.LocalArtistEntity
import com.harmonixia.android.data.local.entity.LocalTrackEntity

@Database(
    entities = [LocalTrackEntity::class, LocalAlbumEntity::class, LocalArtistEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocalMediaDatabase : RoomDatabase() {
    abstract fun localTrackDao(): LocalTrackDao

    abstract fun localAlbumDao(): LocalAlbumDao

    abstract fun localArtistDao(): LocalArtistDao

    companion object {
        const val DATABASE_NAME = "local_media_database"
    }
}
