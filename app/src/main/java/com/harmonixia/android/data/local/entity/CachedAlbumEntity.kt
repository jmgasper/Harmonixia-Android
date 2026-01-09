package com.harmonixia.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_albums",
    indices = [
        Index(value = ["sortIndex"]),
        Index(value = ["syncId"])
    ]
)
data class CachedAlbumEntity(
    @PrimaryKey
    val cacheKey: String,
    val itemId: String,
    val provider: String,
    val uri: String,
    val name: String,
    val artistsJson: String,
    val imageUrl: String?,
    val albumType: String,
    val trackCount: Int,
    val addedAt: String?,
    val lastPlayed: String?,
    val sortIndex: Int,
    val syncId: Long
)
