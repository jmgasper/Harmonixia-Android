package com.harmonixia.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.harmonixia.android.data.local.dao.CachedAlbumDao
import com.harmonixia.android.data.local.dao.CachedArtistDao
import com.harmonixia.android.data.local.dao.LocalAlbumDao
import com.harmonixia.android.data.local.dao.LocalArtistDao
import com.harmonixia.android.data.local.dao.LocalTrackDao
import com.harmonixia.android.data.local.entity.CachedAlbumEntity
import com.harmonixia.android.data.local.entity.CachedArtistEntity
import com.harmonixia.android.data.local.entity.LocalAlbumEntity
import com.harmonixia.android.data.local.entity.LocalArtistEntity
import com.harmonixia.android.data.local.entity.LocalTrackEntity

@Database(
    entities = [
        LocalTrackEntity::class,
        LocalAlbumEntity::class,
        LocalArtistEntity::class,
        CachedAlbumEntity::class,
        CachedArtistEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LocalMediaDatabase : RoomDatabase() {
    abstract fun localTrackDao(): LocalTrackDao

    abstract fun localAlbumDao(): LocalAlbumDao

    abstract fun localArtistDao(): LocalArtistDao

    abstract fun cachedAlbumDao(): CachedAlbumDao

    abstract fun cachedArtistDao(): CachedArtistDao

    companion object {
        const val DATABASE_NAME = "local_media_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE local_albums ADD COLUMN firstTrackPath TEXT"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_albums (
                        cacheKey TEXT NOT NULL,
                        itemId TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        name TEXT NOT NULL,
                        artistsJson TEXT NOT NULL,
                        imageUrl TEXT,
                        albumType TEXT NOT NULL,
                        trackCount INTEGER NOT NULL,
                        addedAt TEXT,
                        lastPlayed TEXT,
                        sortIndex INTEGER NOT NULL,
                        syncId INTEGER NOT NULL,
                        PRIMARY KEY(cacheKey)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_cached_albums_sortIndex ON cached_albums(sortIndex)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_cached_albums_syncId ON cached_albums(syncId)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_artists (
                        cacheKey TEXT NOT NULL,
                        itemId TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        name TEXT NOT NULL,
                        sortName TEXT,
                        imageUrl TEXT,
                        sortIndex INTEGER NOT NULL,
                        syncId INTEGER NOT NULL,
                        PRIMARY KEY(cacheKey)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_cached_artists_sortIndex ON cached_artists(sortIndex)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_cached_artists_syncId ON cached_artists(syncId)"
                )
            }
        }
    }
}
