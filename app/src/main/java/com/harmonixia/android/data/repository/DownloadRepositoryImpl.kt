package com.harmonixia.android.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.room.withTransaction
import com.harmonixia.android.data.local.DownloadDatabase
import com.harmonixia.android.data.local.entity.DownloadedAlbumWithTracks
import com.harmonixia.android.data.local.entity.DownloadedTrackEntity
import com.harmonixia.android.data.local.entity.PlaylistTrackCrossRef
import com.harmonixia.android.data.local.mapper.toDomain
import com.harmonixia.android.data.local.mapper.toDownloadedEntity
import com.harmonixia.android.data.local.mapper.toEntity
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.DownloadProgress
import com.harmonixia.android.domain.model.DownloadStatus
import com.harmonixia.android.domain.model.DownloadedTrack
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.model.downloadId
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.util.DownloadFileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDatabase: DownloadDatabase,
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : DownloadRepository {
    private val trackDao = downloadDatabase.downloadedTrackDao()
    private val albumDao = downloadDatabase.downloadedAlbumDao()
    private val playlistDao = downloadDatabase.downloadedPlaylistDao()
    private val fileManager = DownloadFileManager(context)

    private val progressState = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())

    override suspend fun queueTrackDownload(
        track: Track,
        albumId: String?,
        playlistIds: List<String>
    ): Result<Unit> {
        return runCatching {
            val entity = buildDownloadedTrackEntity(track, albumId, playlistIds)
            trackDao.insert(entity)
            seedProgress(entity.downloadId)
        }
    }

    override suspend fun queueAlbumDownload(album: Album, tracks: List<Track>): Result<Unit> {
        return runCatching {
            val coverArtPath = if (!album.imageUrl.isNullOrBlank()) {
                generateCoverArtPath(album.itemId, album.provider)
            } else {
                null
            }
            val albumEntity = album.toDownloadedEntity(
                trackCount = tracks.size,
                localCoverArtPath = coverArtPath,
                downloadedAt = System.currentTimeMillis()
            )
            val trackEntities = mutableListOf<DownloadedTrackEntity>()
            for (track in tracks) {
                trackEntities.add(buildDownloadedTrackEntity(track, album.downloadId, emptyList()))
            }
            downloadDatabase.withTransaction {
                albumDao.insert(albumEntity)
                for (track in trackEntities) {
                    trackDao.insert(track)
                }
            }
            trackEntities.forEach { seedProgress(it.downloadId) }
        }
    }

    override suspend fun queuePlaylistDownload(playlist: Playlist, tracks: List<Track>): Result<Unit> {
        return runCatching {
            val coverArtPath = if (!playlist.imageUrl.isNullOrBlank()) {
                generateCoverArtPath(playlist.itemId, playlist.provider)
            } else {
                null
            }
            val playlistEntity = playlist.toDownloadedEntity(
                trackCount = tracks.size,
                localCoverArtPath = coverArtPath,
                downloadedAt = System.currentTimeMillis()
            )
            val trackEntities = mutableListOf<DownloadedTrackEntity>()
            for (track in tracks) {
                trackEntities.add(buildDownloadedTrackEntity(track, null, listOf(playlist.downloadId)))
            }
            val refs = tracks.map { track ->
                PlaylistTrackCrossRef(playlistId = playlist.downloadId, trackId = track.downloadId)
            }
            downloadDatabase.withTransaction {
                playlistDao.insert(playlistEntity)
                for (track in trackEntities) {
                    trackDao.insert(track)
                }
                playlistDao.insertTrackRefs(refs)
            }
            trackEntities.forEach { seedProgress(it.downloadId) }
        }
    }

    override fun getDownloadStatus(trackId: String): Flow<DownloadStatus?> {
        val normalized = trackId.trim()
        if (normalized.isBlank()) return flowOf(null)
        return trackDao.getTrack(normalized)
            .map { entity -> entity?.toDomain()?.downloadStatus }
            .flatMapLatest { status ->
                if (status != null) {
                    flowOf(status)
                } else {
                    val itemId = parseItemIdFromDownloadId(normalized) ?: return@flatMapLatest flowOf(null)
                    trackDao.getTracksByItemId(itemId)
                        .map { matches -> resolveStatusFromItemMatches(normalized, matches) }
                }
            }
    }

    override fun getDownloadProgress(trackId: String): Flow<DownloadProgress?> {
        return progressState.map { it[trackId] }.distinctUntilChanged()
    }

    override fun getAllPendingDownloads(): Flow<List<DownloadedTrack>> {
        return trackDao.getTracksByStatus(DownloadStatus.PENDING.name)
            .map { tracks -> tracks.map { it.toDomain() } }
    }

    override fun getAllInProgressDownloads(): Flow<List<DownloadedTrack>> {
        return trackDao.getTracksByStatus(DownloadStatus.IN_PROGRESS.name)
            .map { tracks -> tracks.map { it.toDomain() } }
    }

    override fun getAllCompletedDownloads(): Flow<List<DownloadedTrack>> {
        return trackDao.getTracksByStatus(DownloadStatus.COMPLETED.name)
            .map { tracks -> tracks.map { it.toDomain() } }
    }

    override fun getDownloadedAlbums(): Flow<List<Album>> {
        return albumDao.getAlbumsWithTracks().map { albums ->
            albums.map { entry ->
                entry.album.toDomain().copy(imageUrl = resolveAlbumArtwork(entry))
            }
        }
    }

    override fun getDownloadedPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getPlaylists().map { playlists ->
            playlists.map { entity ->
                val resolvedArtwork = resolvePlaylistArtwork(entity.localCoverArtPath, entity.imageUrl)
                entity.toDomain().copy(imageUrl = resolvedArtwork)
            }
        }
    }

    override fun getDownloadedTracks(): Flow<List<Track>> {
        return trackDao.getTracksByStatus(DownloadStatus.COMPLETED.name)
            .map { tracks -> tracks.map { it.toDomain().track } }
    }

    override suspend fun isTrackDownloaded(trackId: String): Boolean {
        val track = trackDao.getTrack(trackId).first()
        return track?.downloadStatus == DownloadStatus.COMPLETED.name
    }

    override suspend fun isAlbumDownloaded(albumId: String): Boolean {
        return albumDao.exists(albumId)
    }

    override suspend fun isPlaylistDownloaded(playlistId: String): Boolean {
        return playlistDao.exists(playlistId)
    }

    override suspend fun getLocalFilePath(trackId: String): String? {
        return trackDao.getTrack(trackId).first()?.localFilePath
    }

    override suspend fun getDownloadedTrack(trackId: String): DownloadedTrack? {
        return trackDao.getTrack(trackId).first()?.toDomain()
    }

    override suspend fun updateDownloadStatus(
        trackId: String,
        status: DownloadStatus
    ): Result<Unit> {
        return runCatching {
            val track = trackDao.getTrack(trackId).first() ?: return@runCatching
            trackDao.update(track.copy(downloadStatus = status.name))
        }
    }

    override suspend fun updateDownloadProgress(
        trackId: String,
        progress: DownloadProgress
    ): Result<Unit> {
        return runCatching {
            progressState.update { current ->
                current + (trackId to progress.copy(trackId = trackId))
            }
        }
    }

    override suspend fun updateDownloadCompleted(trackId: String, fileSize: Long): Result<Unit> {
        return runCatching {
            val track = trackDao.getTrack(trackId).first() ?: return@runCatching
            val updated = track.copy(
                downloadStatus = DownloadStatus.COMPLETED.name,
                downloadedAt = System.currentTimeMillis(),
                fileSize = fileSize
            )
            trackDao.update(updated)
        }
    }

    override suspend fun deleteTrackDownload(trackId: String): Result<Unit> {
        return runCatching {
            val track = trackDao.getTrack(trackId).first() ?: return@runCatching
            fileManager.deleteFile(track.localFilePath).getOrThrow()
            if (!track.coverArtPath.isNullOrBlank()) {
                fileManager.deleteFile(track.coverArtPath).getOrThrow()
            }
            downloadDatabase.withTransaction {
                playlistDao.deleteTrackRefsForTrack(track.downloadId)
                trackDao.delete(track)
            }
            progressState.update { it - track.downloadId }
        }
    }

    override suspend fun deleteAlbumDownload(albumId: String): Result<Unit> {
        return runCatching {
            val album = albumDao.getAlbumOnce(albumId)
            val tracks = trackDao.getTracksByAlbum(albumId).first()
            val albumCoverArtPath = album?.localCoverArtPath
            if (!albumCoverArtPath.isNullOrBlank()) {
                fileManager.deleteFile(albumCoverArtPath).getOrThrow()
            }
            for (track in tracks) {
                fileManager.deleteFile(track.localFilePath).getOrThrow()
                if (!track.coverArtPath.isNullOrBlank()) {
                    fileManager.deleteFile(track.coverArtPath).getOrThrow()
                }
            }
            val trackIds = tracks.map { it.downloadId }
            downloadDatabase.withTransaction {
                for (trackId in trackIds) {
                    playlistDao.deleteTrackRefsForTrack(trackId)
                }
                trackDao.deleteByAlbum(albumId)
                albumDao.deleteById(albumId)
            }
            progressState.update { it - trackIds }
        }
    }

    override suspend fun deletePlaylistDownload(playlistId: String): Result<Unit> {
        return runCatching {
            val playlist = playlistDao.getPlaylistOnce(playlistId)
            val playlistCoverArtPath = playlist?.localCoverArtPath
            if (!playlistCoverArtPath.isNullOrBlank()) {
                fileManager.deleteFile(playlistCoverArtPath).getOrThrow()
            }
            val trackIds = playlistDao.getTrackIdsForPlaylist(playlistId)
            downloadDatabase.withTransaction {
                playlistDao.deleteTrackRefsForPlaylist(playlistId)
                playlistDao.deleteById(playlistId)
            }
            if (trackIds.isEmpty()) return@runCatching
            val albumlessTracks = trackDao.getTracksWithoutAlbum(trackIds)
            if (albumlessTracks.isEmpty()) return@runCatching
            val albumlessTrackIds = albumlessTracks.map { it.downloadId }
            val remainingRefs = playlistDao.getTrackIdsWithPlaylistRefs(albumlessTrackIds).toSet()
            val orphanTracks = albumlessTracks.filter { it.downloadId !in remainingRefs }
            if (orphanTracks.isEmpty()) return@runCatching
            for (track in orphanTracks) {
                fileManager.deleteFile(track.localFilePath).getOrThrow()
                if (!track.coverArtPath.isNullOrBlank()) {
                    fileManager.deleteFile(track.coverArtPath).getOrThrow()
                }
            }
            downloadDatabase.withTransaction {
                for (track in orphanTracks) {
                    trackDao.delete(track)
                }
            }
            progressState.update { it - orphanTracks.map { track -> track.downloadId } }
        }
    }

    override suspend fun deleteAllDownloadedMedia(): Result<Unit> {
        return runCatching {
            val tracks = trackDao.getAllOnce()
            val albums = albumDao.getAlbums().first()
            val playlists = playlistDao.getPlaylists().first()
            for (track in tracks) {
                fileManager.deleteFile(track.localFilePath).getOrThrow()
                if (!track.coverArtPath.isNullOrBlank()) {
                    fileManager.deleteFile(track.coverArtPath).getOrThrow()
                }
            }
            for (album in albums) {
                if (!album.localCoverArtPath.isNullOrBlank()) {
                    fileManager.deleteFile(album.localCoverArtPath).getOrThrow()
                }
            }
            for (playlist in playlists) {
                if (!playlist.localCoverArtPath.isNullOrBlank()) {
                    fileManager.deleteFile(playlist.localCoverArtPath).getOrThrow()
                }
            }
        }
    }

    override suspend fun deleteAllDownloads(): Result<Unit> {
        return runCatching {
            withContext(ioDispatcher) {
                downloadDatabase.clearAllTables()
            }
            progressState.value = emptyMap()
        }
    }

    private suspend fun buildDownloadedTrackEntity(
        track: Track,
        albumDownloadId: String?,
        playlistDownloadIds: List<String>
    ): DownloadedTrackEntity {
        val filePath = generateTrackFilePath(track)
        val coverArtPath = if (!track.imageUrl.isNullOrBlank()) {
            generateCoverArtPath(track.itemId, track.provider)
        } else {
            null
        }
        val downloadedTrack = DownloadedTrack(
            track = track,
            localFilePath = filePath,
            downloadStatus = DownloadStatus.PENDING,
            downloadedAt = null,
            fileSize = null,
            coverArtPath = coverArtPath,
            albumId = albumDownloadId,
            playlistIds = playlistDownloadIds
        )
        return downloadedTrack.toEntity()
    }

    private suspend fun generateTrackFilePath(track: Track): String {
        val extension = resolveTrackExtension(track)
        return withContext(ioDispatcher) {
            fileManager.generateTrackFilePath(track.itemId, track.provider, extension)
        }
    }

    private suspend fun generateCoverArtPath(itemId: String, provider: String): String {
        return withContext(ioDispatcher) {
            fileManager.generateCoverArtPath(itemId, provider)
        }
    }

    private fun resolveTrackExtension(track: Track): String {
        val uri = runCatching { Uri.parse(track.uri) }.getOrNull()
        val mimeExtension = runCatching {
            uri?.let { context.contentResolver.getType(it) }
        }.getOrNull()?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        val uriExtension = MimeTypeMap.getFileExtensionFromUrl(track.uri).takeIf { it.isNotBlank() }
        return sanitizeExtension(mimeExtension ?: uriExtension ?: DEFAULT_AUDIO_EXTENSION)
    }

    private fun sanitizeExtension(rawExtension: String): String {
        val cleaned = rawExtension.trim().trimStart('.')
        if (cleaned.isBlank()) return DEFAULT_AUDIO_EXTENSION
        return cleaned.lowercase(Locale.US)
    }

    private fun seedProgress(trackId: String) {
        progressState.update { current ->
            if (current.containsKey(trackId)) current
            else current + (trackId to DownloadProgress(trackId = trackId))
        }
    }

    private fun resolveAlbumArtwork(entry: DownloadedAlbumWithTracks): String? {
        val localCover = entry.album.localCoverArtPath?.takeIf { fileManager.fileExists(it) }
        if (localCover != null) return localCover
        val trackCover = entry.tracks.asSequence()
            .mapNotNull { it.coverArtPath }
            .firstOrNull { fileManager.fileExists(it) }
        return trackCover ?: entry.album.imageUrl
    }

    private fun resolvePlaylistArtwork(localCoverArtPath: String?, fallbackUrl: String?): String? {
        return localCoverArtPath?.takeIf { fileManager.fileExists(it) } ?: fallbackUrl
    }

    private fun parseItemIdFromDownloadId(downloadId: String): String? {
        val delimiterIndex = downloadId.lastIndexOf('-')
        if (delimiterIndex < 0) return downloadId
        if (delimiterIndex == 0) return null
        return downloadId.substring(0, delimiterIndex)
    }

    private fun parseProviderFromDownloadId(downloadId: String): String? {
        val delimiterIndex = downloadId.lastIndexOf('-')
        if (delimiterIndex < 0 || delimiterIndex >= downloadId.lastIndex) return null
        return downloadId.substring(delimiterIndex + 1)
    }

    private fun resolveStatusFromItemMatches(
        downloadId: String,
        matches: List<DownloadedTrackEntity>
    ): DownloadStatus? {
        if (matches.isEmpty()) return null
        val provider = parseProviderFromDownloadId(downloadId)
        val match = when {
            provider.isNullOrBlank() -> matches.firstOrNull { it.provider.isBlank() } ?: matches.first()
            else -> matches.firstOrNull { it.provider == provider }
                ?: matches.firstOrNull { it.provider.isBlank() }
                ?: matches.first()
        }
        return match?.toDomain()?.downloadStatus
    }

    private companion object {
        private const val DEFAULT_AUDIO_EXTENSION = "bin"
    }
}
