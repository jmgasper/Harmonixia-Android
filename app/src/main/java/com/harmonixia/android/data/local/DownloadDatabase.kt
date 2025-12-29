package com.harmonixia.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.harmonixia.android.data.local.dao.DownloadedAlbumDao
import com.harmonixia.android.data.local.dao.DownloadedPlaylistDao
import com.harmonixia.android.data.local.dao.DownloadedTrackDao
import com.harmonixia.android.data.local.entity.DownloadedAlbumEntity
import com.harmonixia.android.data.local.entity.DownloadedPlaylistEntity
import com.harmonixia.android.data.local.entity.DownloadedTrackEntity
import com.harmonixia.android.data.local.entity.PlaylistTrackCrossRef

@Database(
    entities = [
        DownloadedTrackEntity::class,
        DownloadedAlbumEntity::class,
        DownloadedPlaylistEntity::class,
        PlaylistTrackCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadedTrackDao(): DownloadedTrackDao

    abstract fun downloadedAlbumDao(): DownloadedAlbumDao

    abstract fun downloadedPlaylistDao(): DownloadedPlaylistDao
}
