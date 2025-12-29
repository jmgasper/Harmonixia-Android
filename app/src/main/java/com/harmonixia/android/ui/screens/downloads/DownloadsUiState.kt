package com.harmonixia.android.ui.screens.downloads

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.DownloadProgress
import com.harmonixia.android.domain.model.DownloadedTrack
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track

sealed interface DownloadsUiState {
    data object Loading : DownloadsUiState
    data object Empty : DownloadsUiState
    data class Error(val message: String) : DownloadsUiState
    data class Success(
        val overallProgress: DownloadProgress,
        val inProgressDownloads: List<DownloadItemUi>,
        val downloadedPlaylists: List<Playlist>,
        val downloadedAlbums: List<Album>,
        val downloadedTracks: List<Track>
    ) : DownloadsUiState
}

data class DownloadItemUi(
    val download: DownloadedTrack,
    val progress: DownloadProgress
)
