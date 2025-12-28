package com.harmonixia.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LibraryData(
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)
