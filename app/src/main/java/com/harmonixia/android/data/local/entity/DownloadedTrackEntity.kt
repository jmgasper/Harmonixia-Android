package com.harmonixia.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloaded_tracks",
    indices = [
        Index(value = ["downloadId"]),
        Index(value = ["albumId"]),
        Index(value = ["downloadStatus"])
    ]
)
data class DownloadedTrackEntity(
    @PrimaryKey
    val downloadId: String,
    val itemId: String,
    val provider: String,
    val uri: String,
    val trackNumber: Int,
    val title: String,
    val artist: String,
    val album: String,
    val lengthSeconds: Int,
    val imageUrl: String?,
    val quality: String?,
    val localFilePath: String,
    val downloadStatus: String,
    val downloadedAt: Long?,
    val fileSize: Long?,
    val coverArtPath: String?,
    val albumId: String?,
    val playlistIds: String?
)
