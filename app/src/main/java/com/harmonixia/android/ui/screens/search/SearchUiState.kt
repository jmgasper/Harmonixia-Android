package com.harmonixia.android.ui.screens.search

import com.harmonixia.android.domain.model.SearchResults

sealed interface SearchUiState {
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    data class Success(
        val query: String,
        val results: SearchResults,
        val isEmpty: Boolean,
        val albumLimit: Int,
        val artistLimit: Int,
        val playlistLimit: Int,
        val trackLimit: Int
    ) : SearchUiState

    data class Error(
        val message: String
    ) : SearchUiState
}
