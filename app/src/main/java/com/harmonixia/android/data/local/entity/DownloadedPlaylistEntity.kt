package com.harmonixia.android.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "downloaded_playlists")
data class DownloadedPlaylistEntity(
    @PrimaryKey
    val downloadId: String,
    val playlistId: String,
    val provider: String,
    val name: String,
    val owner: String?,
    val imageUrl: String?,
    val localCoverArtPath: String?,
    val downloadedAt: Long?,
    val trackCount: Int
)

data class DownloadedPlaylistWithTracks(
    @Embedded
    val playlist: DownloadedPlaylistEntity,
    @Relation(
        parentColumn = "downloadId",
        entityColumn = "downloadId",
        associateBy = Junction(
            value = PlaylistTrackCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "trackId"
        )
    )
    val tracks: List<DownloadedTrackEntity>
)
