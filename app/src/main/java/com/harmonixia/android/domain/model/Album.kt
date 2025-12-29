package com.harmonixia.android.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Album(
    @SerialName("item_id")
    val itemId: String,
    val provider: String,
    val uri: String,
    val name: String,
    val artists: List<String> = emptyList(),
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("album_type")
    val albumType: AlbumType = AlbumType.UNKNOWN,
    @SerialName("provider_mappings")
    val providerMappings: List<ProviderMapping> = emptyList(),
    @SerialName("added_at")
    val addedAt: String? = null,
    @SerialName("last_played")
    val lastPlayed: String? = null
)

val Album.downloadId: String
    get() = "$itemId-$provider"

@Serializable
enum class AlbumType {
    @SerialName("album")
    ALBUM,
    @SerialName("single")
    SINGLE,
    @SerialName("compilation")
    COMPILATION,
    @SerialName("ep")
    EP,
    @SerialName("unknown")
    UNKNOWN
}
