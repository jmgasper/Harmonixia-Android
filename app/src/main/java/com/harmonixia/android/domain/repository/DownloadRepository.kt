package com.harmonixia.android.domain.repository

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.DownloadProgress
import com.harmonixia.android.domain.model.DownloadStatus
import com.harmonixia.android.domain.model.DownloadedTrack
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    suspend fun queueTrackDownload(
        track: Track,
        albumId: String?,
        playlistIds: List<String>
    ): Result<Unit>

    suspend fun queueAlbumDownload(album: Album, tracks: List<Track>): Result<Unit>

    suspend fun queuePlaylistDownload(playlist: Playlist, tracks: List<Track>): Result<Unit>

    fun getDownloadStatus(trackId: String): Flow<DownloadStatus?>

    fun getDownloadProgress(trackId: String): Flow<DownloadProgress?>

    fun getAllPendingDownloads(): Flow<List<DownloadedTrack>>

    fun getAllInProgressDownloads(): Flow<List<DownloadedTrack>>

    fun getAllCompletedDownloads(): Flow<List<DownloadedTrack>>

    fun getDownloadedAlbums(): Flow<List<Album>>

    fun getDownloadedPlaylists(): Flow<List<Playlist>>

    fun getDownloadedTracks(): Flow<List<Track>>

    suspend fun isTrackDownloaded(trackId: String): Boolean

    suspend fun isAlbumDownloaded(albumId: String): Boolean

    suspend fun isPlaylistDownloaded(playlistId: String): Boolean

    suspend fun getLocalFilePath(trackId: String): String?

    suspend fun getDownloadedTrack(trackId: String): DownloadedTrack?

    suspend fun updateDownloadStatus(trackId: String, status: DownloadStatus): Result<Unit>

    suspend fun updateDownloadProgress(trackId: String, progress: DownloadProgress): Result<Unit>

    suspend fun updateDownloadCompleted(trackId: String, fileSize: Long): Result<Unit>

    suspend fun deleteTrackDownload(trackId: String): Result<Unit>

    suspend fun deleteAlbumDownload(albumId: String): Result<Unit>

    suspend fun deletePlaylistDownload(playlistId: String): Result<Unit>

    suspend fun deleteAllDownloadedMedia(): Result<Unit>

    suspend fun deleteAllDownloads(): Result<Unit>
}
