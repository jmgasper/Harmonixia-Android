package com.harmonixia.android.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "downloaded_albums")
data class DownloadedAlbumEntity(
    @PrimaryKey
    val downloadId: String,
    val albumId: String,
    val provider: String,
    val name: String,
    val artists: String,
    val imageUrl: String?,
    val localCoverArtPath: String?,
    val downloadedAt: Long?,
    val trackCount: Int
)

data class DownloadedAlbumWithTracks(
    @Embedded
    val album: DownloadedAlbumEntity,
    @Relation(
        parentColumn = "downloadId",
        entityColumn = "albumId"
    )
    val tracks: List<DownloadedTrackEntity>
)
