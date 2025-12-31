package com.harmonixia.android.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RepeatMode {
    @SerialName("off")
    OFF,
    @SerialName("one")
    ONE,
    @SerialName("all")
    ALL
}
