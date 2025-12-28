package com.harmonixia.android.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PlaybackState {
    @SerialName("idle")
    IDLE,
    @SerialName("playing")
    PLAYING,
    @SerialName("paused")
    PAUSED
}
