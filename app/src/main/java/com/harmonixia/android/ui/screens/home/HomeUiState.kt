package com.harmonixia.android.ui.screens.home

import com.harmonixia.android.domain.model.Album

sealed interface HomeUiState {
    object Loading : HomeUiState

    data class Success(
        val recentlyPlayed: List<Album>,
        val recentlyAdded: List<Album>
    ) : HomeUiState

    data class Error(
        val message: String
    ) : HomeUiState

    data class Offline(
        val albumCount: Int,
        val artistCount: Int,
        val trackCount: Int
    ) : HomeUiState

    object Empty : HomeUiState
}
