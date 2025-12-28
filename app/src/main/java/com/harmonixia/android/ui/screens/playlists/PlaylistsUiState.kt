package com.harmonixia.android.ui.screens.playlists

import androidx.paging.PagingData
import com.harmonixia.android.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

sealed interface PlaylistsUiState {
    data object Loading : PlaylistsUiState

    data class Success(
        val playlists: Flow<PagingData<Playlist>>
    ) : PlaylistsUiState

    data class Error(
        val message: String
    ) : PlaylistsUiState

    data object Empty : PlaylistsUiState
}
