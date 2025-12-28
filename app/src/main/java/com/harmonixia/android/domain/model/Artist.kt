package com.harmonixia.android.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Artist(
    @SerialName("item_id")
    val itemId: String,
    val provider: String,
    val uri: String,
    val name: String,
    @SerialName("sort_name")
    val sortName: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null
)
