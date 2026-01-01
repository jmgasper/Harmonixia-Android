package com.harmonixia.android.ui.screens.albums

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Track

sealed interface AlbumDetailUiState {
    object Loading : AlbumDetailUiState
    object Metadata : AlbumDetailUiState

    data class Cached(
        val album: Album,
        val tracks: List<Track>,
        val isRefreshing: Boolean = false
    ) : AlbumDetailUiState

    data class Success(
        val album: Album,
        val tracks: List<Track>,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : AlbumDetailUiState

    data class Error(
        val message: String
    ) : AlbumDetailUiState

    object Empty : AlbumDetailUiState
}
