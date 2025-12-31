package com.harmonixia.android.data.local.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER

@Entity(
    tableName = "local_artists",
    indices = [Index(value = ["name"], unique = true)]
)
data class LocalArtistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val dateAdded: Long
)

fun LocalArtistEntity.toArtist(): Artist {
    val trimmedName = name.trim()
    val encodedId = Uri.encode(trimmedName)
    return Artist(
        itemId = encodedId,
        provider = OFFLINE_PROVIDER,
        uri = "offline:artist:$encodedId",
        name = trimmedName,
        sortName = trimmedName.lowercase(),
        imageUrl = null
    )
}
