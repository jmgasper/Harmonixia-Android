package com.harmonixia.android.domain.model

data class PlaybackContext(
    val source: PlaybackSource,
    val title: String? = null
)

enum class PlaybackSource {
    ALBUM,
    PLAYLIST,
    HOME
}
