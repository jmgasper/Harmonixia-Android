package com.harmonixia.android.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Queue(
    @SerialName("queue_id")
    val queueId: String,
    val state: PlaybackState = PlaybackState.IDLE,
    @SerialName("repeat_mode")
    val repeatMode: RepeatMode = RepeatMode.OFF,
    @SerialName("shuffle")
    val shuffle: Boolean = false,
    @SerialName("current_item")
    val currentItem: Track? = null,
    @SerialName("current_index")
    val currentIndex: Int = 0,
    @SerialName("elapsed_time")
    val elapsedTime: Int = 0,
    @SerialName("elapsed_time_last_updated")
    val elapsedTimeLastUpdated: Double? = null,
    val items: List<Track> = emptyList()
)
