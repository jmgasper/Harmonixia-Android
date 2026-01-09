package com.harmonixia.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_artists",
    indices = [
        Index(value = ["sortIndex"]),
        Index(value = ["syncId"])
    ]
)
data class CachedArtistEntity(
    @PrimaryKey
    val cacheKey: String,
    val itemId: String,
    val provider: String,
    val uri: String,
    val name: String,
    val sortName: String?,
    val imageUrl: String?,
    val sortIndex: Int,
    val syncId: Long
)
