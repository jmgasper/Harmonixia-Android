package com.harmonixia.android.ui.screens.albums

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Track

sealed interface AlbumDetailUiState {
    object Loading : AlbumDetailUiState

    data class Success(
        val album: Album,
        val tracks: List<Track>
    ) : AlbumDetailUiState

    data class Error(
        val message: String
    ) : AlbumDetailUiState

    object Empty : AlbumDetailUiState
}
