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
    @SerialName("provider_mappings")
    val providerMappings: List<ProviderMapping> = emptyList(),
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
    val quality: String? = null,
    @SerialName("available")
    val isAvailable: Boolean = true,
    @SerialName("is_favorite")
    val isFavorite: Boolean = false,
    val albumItemId: String = "",
    val albumProvider: String = "",
    val albumProviderMappings: List<ProviderMapping> = emptyList(),
    val albumUri: String = ""
)

val Track.downloadId: String
    get() = "$itemId-$provider"
