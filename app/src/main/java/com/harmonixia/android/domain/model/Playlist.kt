package com.harmonixia.android.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Playlist(
    @SerialName("item_id")
    val itemId: String,
    val provider: String,
    val uri: String,
    val name: String,
    val owner: String? = null,
    @SerialName("is_editable")
    val isEditable: Boolean = false,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("track_count")
    val trackCount: Int = 0
)

val Playlist.downloadId: String
    get() = "$itemId-$provider"
