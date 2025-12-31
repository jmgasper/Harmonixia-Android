package com.harmonixia.android.data.local.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER

@Entity(
    tableName = "local_tracks",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["albumArtist"])
    ]
)
data class LocalTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val filePath: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String?,
    val trackNumber: Int,
    val durationMs: Long,
    val mimeType: String,
    val fileSize: Long,
    val lastModified: Long,
    val dateAdded: Long
)

fun LocalTrackEntity.toTrack(): Track {
    val encodedId = Uri.encode(filePath)
    val lengthSeconds = (durationMs / 1000L)
        .coerceAtLeast(0L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
    return Track(
        itemId = encodedId,
        provider = OFFLINE_PROVIDER,
        uri = filePath,
        trackNumber = trackNumber,
        title = title,
        artist = artist,
        album = album,
        lengthSeconds = lengthSeconds
    )
}
