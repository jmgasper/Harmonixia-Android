package com.harmonixia.android.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class RecommendationSection(
    val title: String,
    val items: List<RecommendationItem> = emptyList()
)

@Immutable
data class RecommendationItem(
    val mediaType: RecommendationMediaType,
    val title: String,
    val subtitle: String = "",
    val imageUrl: String? = null,
    val album: Album? = null,
    val playlist: Playlist? = null,
    val artist: Artist? = null,
    val track: Track? = null
)

enum class RecommendationMediaType {
    ALBUM,
    PLAYLIST,
    ARTIST,
    TRACK,
    OTHER
}
