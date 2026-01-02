package com.harmonixia.android.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Player(
    @SerialName("player_id")
    val playerId: String,
    val name: String,
    val available: Boolean = false,
    val enabled: Boolean = false,
    @SerialName("playback_state")
    val playbackState: PlaybackState = PlaybackState.IDLE,
    @SerialName("volume_level")
    val volume: Int = 0,
    @SerialName("volume_muted")
    val volumeMuted: Boolean? = null,
    val deviceManufacturer: String? = null,
    val deviceModel: String? = null
)
