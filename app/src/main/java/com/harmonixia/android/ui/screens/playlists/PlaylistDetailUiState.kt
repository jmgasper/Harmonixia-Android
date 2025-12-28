package com.harmonixia.android.ui.screens.playlists

import com.harmonixia.android.domain.model.Track

sealed interface PlaylistDetailUiState {
    object Loading : PlaylistDetailUiState

    data class Success(
        val tracks: List<Track>
    ) : PlaylistDetailUiState

    data class Error(
        val message: String
    ) : PlaylistDetailUiState

    object Empty : PlaylistDetailUiState
}
