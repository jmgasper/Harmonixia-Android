package com.harmonixia.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.harmonixia.android.data.local.dao.LocalAlbumDao
import com.harmonixia.android.data.local.dao.LocalArtistDao
import com.harmonixia.android.data.local.dao.LocalTrackDao
import com.harmonixia.android.data.local.entity.LocalAlbumEntity
import com.harmonixia.android.data.local.entity.LocalArtistEntity
import com.harmonixia.android.data.local.entity.LocalTrackEntity

@Database(
    entities = [LocalTrackEntity::class, LocalAlbumEntity::class, LocalArtistEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LocalMediaDatabase : RoomDatabase() {
    abstract fun localTrackDao(): LocalTrackDao

    abstract fun localAlbumDao(): LocalAlbumDao

    abstract fun localArtistDao(): LocalArtistDao

    companion object {
        const val DATABASE_NAME = "local_media_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE local_albums ADD COLUMN firstTrackPath TEXT"
                )
            }
        }
    }
}
