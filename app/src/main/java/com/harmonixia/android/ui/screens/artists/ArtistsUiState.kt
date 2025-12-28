package com.harmonixia.android.ui.screens.artists

import androidx.paging.PagingData
import com.harmonixia.android.domain.model.Artist
import kotlinx.coroutines.flow.Flow

sealed interface ArtistsUiState {
    data object Loading : ArtistsUiState

    data class Success(
        val artists: Flow<PagingData<Artist>>
    ) : ArtistsUiState

    data class Error(
        val message: String
    ) : ArtistsUiState

    data object Empty : ArtistsUiState
}
