package com.harmonixia.android.ui.screens.artists

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist

sealed interface ArtistDetailUiState {
    object Loading : ArtistDetailUiState

    data class Success(
        val artist: Artist,
        val libraryAlbums: List<Album>,
        val allAlbums: List<Album>
    ) : ArtistDetailUiState

    data class Error(
        val message: String
    ) : ArtistDetailUiState

    object Empty : ArtistDetailUiState
}
