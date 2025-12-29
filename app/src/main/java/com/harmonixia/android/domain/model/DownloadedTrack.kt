package com.harmonixia.android.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class DownloadedTrack(
    val track: Track,
    val localFilePath: String,
    val downloadStatus: DownloadStatus = DownloadStatus.PENDING,
    val downloadedAt: Long? = null,
    val fileSize: Long? = null,
    val coverArtPath: String? = null,
    val albumId: String? = null,
    val playlistIds: List<String> = emptyList()
)
