package com.harmonixia.android.ui.screens.playlists

import com.harmonixia.android.domain.model.Track

sealed interface PlaylistDetailUiState {
    object Loading : PlaylistDetailUiState
    object Metadata : PlaylistDetailUiState

    data class Cached(
        val tracks: List<Track>,
        val isRefreshing: Boolean = false
    ) : PlaylistDetailUiState

    data class Success(
        val tracks: List<Track>,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : PlaylistDetailUiState

    data class Error(
        val message: String
    ) : PlaylistDetailUiState

    object Empty : PlaylistDetailUiState
}
