package com.harmonixia.android.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Immutable
@Serializable
data class Track(
    @SerialName("item_id")
    val itemId: String,
    val provider: String,
    val uri: String,
    @SerialName("track_number")
    val trackNumber: Int = 0,
    @JsonNames("name", "title")
    val title: String = "",
    @JsonNames("artist_str", "artist")
    val artist: String = "",
    @JsonNames("album", "album_name")
    val album: String = "",
    @SerialName("length_seconds")
    @JsonNames("length_seconds", "duration")
    val lengthSeconds: Int = 0,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val quality: String? = null
)

val Track.downloadId: String
    get() = "$itemId-$provider"
