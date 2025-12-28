package com.harmonixia.android.ui.screens.albums

import androidx.paging.PagingData
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.AlbumType
import kotlinx.coroutines.flow.Flow

sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState

    data class Success(
        val albums: Flow<PagingData<Album>>,
        val selectedAlbumTypes: Set<AlbumType>
    ) : AlbumsUiState

    data class Error(
        val message: String
    ) : AlbumsUiState

    data object Empty : AlbumsUiState
}
