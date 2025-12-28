package com.harmonixia.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResults(
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val tracks: List<Track> = emptyList()
)
