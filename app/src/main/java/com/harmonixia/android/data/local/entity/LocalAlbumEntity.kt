package com.harmonixia.android.data.local.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER

@Entity(
    tableName = "local_albums",
    indices = [Index(value = ["name", "artist"], unique = true)]
)
data class LocalAlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val artist: String,
    val trackCount: Int,
    val totalDurationMs: Long,
    val dateAdded: Long,
    val firstTrackPath: String? = null
)

fun LocalAlbumEntity.toAlbum(): Album {
    val trimmedName = name.trim()
    val trimmedArtist = artist.trim()
    val encodedId = Uri.encode("$trimmedArtist:$trimmedName")
    val artists = if (trimmedArtist.isBlank()) emptyList() else listOf(trimmedArtist)
    val imageUrl = firstTrackPath?.trim()?.takeIf { it.isNotBlank() }
    return Album(
        itemId = encodedId,
        provider = OFFLINE_PROVIDER,
        uri = "offline:album:$encodedId",
        name = trimmedName,
        artists = artists,
        imageUrl = imageUrl
    )
}
