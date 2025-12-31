package com.harmonixia.android.data.local

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.harmonixia.android.data.local.dao.LocalAlbumDao
import com.harmonixia.android.data.local.dao.LocalArtistDao
import com.harmonixia.android.data.local.dao.LocalTrackDao
import com.harmonixia.android.data.local.entity.LocalAlbumEntity
import com.harmonixia.android.data.local.entity.LocalArtistEntity
import com.harmonixia.android.data.local.entity.LocalTrackEntity
import com.harmonixia.android.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalMediaScanner @Inject constructor(
    private val localTrackDao: LocalTrackDao,
    private val localAlbumDao: LocalAlbumDao,
    private val localArtistDao: LocalArtistDao,
    private val localMediaDatabase: LocalMediaDatabase,
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val scannerScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val _scanProgress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    private val pendingTracks = mutableListOf<LocalTrackEntity>()
    private val stagedTracks = mutableListOf<LocalTrackEntity>()
    private val audioFiles = mutableListOf<DocumentFile>()
    private var cachedTracksByPath: Map<String, LocalTrackEntity> = emptyMap()
    private var currentTracksAdded = 0

    init {
        scannerScope.launch {
            _scanProgress.collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning ->
                        Logger.d(TAG, "Scanning: ${progress.current}/${progress.total} files")
                    is ScanProgress.Complete ->
                        Logger.i(TAG, "Scan complete: ${progress.result}")
                    is ScanProgress.Error ->
                        Logger.e(TAG, "Scan error: ${progress.message}")
                    ScanProgress.Idle -> Unit
                }
            }
        }
    }

    fun getScanProgress(): StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    suspend fun scanFolder(folderUri: String): Result<ScanResult> {
        return scannerScope.async {
            scanFolderInternal(folderUri)
        }.await()
    }

    private suspend fun scanFolderInternal(folderUri: String): Result<ScanResult> = withContext(ioDispatcher) {
        if (folderUri.isBlank()) {
            return@withContext failureResult("Local media folder URI is blank")
        }
        val uri = runCatching { Uri.parse(folderUri) }.getOrNull()
            ?: return@withContext failureResult("Invalid local media folder URI")
        val root = DocumentFile.fromTreeUri(context, uri)
            ?: return@withContext failureResult("Unable to access local media folder")
        if (!root.exists() || !root.isDirectory) {
            return@withContext failureResult("Local media folder is not accessible")
        }
        val result = runCatching {
            cachedTracksByPath = localTrackDao.getAllTracks().first().associateBy { it.filePath }
            pendingTracks.clear()
            stagedTracks.clear()
            audioFiles.clear()
            scanDocumentTree(root)
            val totalFiles = audioFiles.size
            var filesScanned = 0
            currentTracksAdded = 0
            _scanProgress.value = ScanProgress.Scanning(current = 0, total = totalFiles)

            for (documentFile in audioFiles) {
                try {
                    processAudioFile(documentFile)
                } catch (exception: Exception) {
                    Logger.w(
                        TAG,
                        "Skipping unreadable audio file: ${documentFile.uri}",
                        exception
                    )
                }
                filesScanned++
                if (filesScanned % PROGRESS_UPDATE_INTERVAL == 0 || filesScanned == totalFiles) {
                    _scanProgress.value = ScanProgress.Scanning(
                        current = filesScanned,
                        total = totalFiles
                    )
                }
            }

            flushPendingTracks()
            val (albumEntities, artistEntities) = aggregateAlbumsAndArtists(stagedTracks)
            persistScanResults(stagedTracks, albumEntities, artistEntities)
            val resultData = ScanResult(
                filesScanned = filesScanned,
                tracksAdded = currentTracksAdded,
                albumsAdded = albumEntities.size,
                artistsAdded = artistEntities.size
            )
            _scanProgress.value = ScanProgress.Complete(resultData)
            resultData
        }
        result.onFailure { throwable ->
            val message = when (throwable) {
                is SecurityException -> "Permission denied for local media folder"
                else -> throwable.message ?: "Unknown scan error"
            }
            Logger.e(TAG, "Local media scan failed", throwable)
            _scanProgress.value = ScanProgress.Error(message)
        }
        result
    }

    private suspend fun scanDocumentTree(documentFile: DocumentFile) {
        val children = documentFile.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                scanDocumentTree(child)
            } else if (child.isFile && resolveAudioMimeType(child) != null) {
                audioFiles.add(child)
            }
        }
    }

    private suspend fun processAudioFile(documentFile: DocumentFile) {
        val mimeType = resolveAudioMimeType(documentFile) ?: return
        val fileUri = documentFile.uri
        val filePath = fileUri.toString()
        val lastModified = documentFile.lastModified()
        val fileSize = documentFile.length()
        val cachedTrack = cachedTracksByPath[filePath]
        val unchanged = cachedTrack != null &&
            cachedTrack.lastModified == lastModified &&
            cachedTrack.fileSize == fileSize

        val trackEntity = if (unchanged) {
            cachedTrack!!.copy(
                id = 0L,
                mimeType = mimeType,
                fileSize = fileSize,
                lastModified = lastModified
            )
        } else {
            val fallbackTitle = documentFile.name
                ?.substringBeforeLast('.')
                ?.ifBlank { null }
                ?: "Unknown"
            val metadata = runCatching { readMetadata(fileUri) }
                .getOrElse { throwable ->
                    Logger.w(TAG, "Failed to read metadata for $filePath", throwable)
                    TrackMetadata(
                        title = fallbackTitle,
                        artist = "Unknown",
                        album = "Unknown",
                        albumArtist = null,
                        trackNumber = 0,
                        durationMs = 0L
                    )
                }
            val title = metadata.title.trim().ifBlank { fallbackTitle }
            val artist = metadata.artist.trim().ifBlank { "Unknown" }
            val album = metadata.album.trim().ifBlank { "Unknown" }
            val albumArtist = metadata.albumArtist
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: artist
            val dateAdded = cachedTrack?.dateAdded
                ?: lastModified.takeIf { it > 0L }
                ?: System.currentTimeMillis()
            LocalTrackEntity(
                filePath = filePath,
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                trackNumber = metadata.trackNumber,
                durationMs = metadata.durationMs,
                mimeType = mimeType,
                fileSize = fileSize,
                lastModified = lastModified,
                dateAdded = dateAdded
            )
        }

        pendingTracks.add(trackEntity)
        currentTracksAdded++
        if (pendingTracks.size >= TRACK_BATCH_SIZE) {
            flushPendingTracks()
        }
    }

    private fun resolveAudioMimeType(documentFile: DocumentFile): String? {
        val declaredType = documentFile.type?.trim().orEmpty()
        if (declaredType.startsWith("audio/")) {
            return declaredType
        }
        val extension = getFileExtension(documentFile) ?: return null
        if (!AUDIO_EXTENSION_MIME_TYPES.containsKey(extension)) {
            return null
        }
        return AUDIO_EXTENSION_MIME_TYPES[extension] ?: DEFAULT_AUDIO_MIME_TYPE
    }

    private fun getFileExtension(documentFile: DocumentFile): String? {
        val name = documentFile.name ?: return null
        val dotIndex = name.lastIndexOf('.')
        if (dotIndex < 1 || dotIndex == name.lastIndex) {
            return null
        }
        return name.substring(dotIndex + 1).lowercase(Locale.US)
    }

    private fun aggregateAlbumsAndArtists(
        tracks: List<LocalTrackEntity>
    ): Pair<List<LocalAlbumEntity>, List<LocalArtistEntity>> {
        if (tracks.isEmpty()) {
            return emptyList<LocalAlbumEntity>() to emptyList()
        }
        val albumStats = mutableMapOf<AlbumKey, AlbumStats>()
        val artistStats = mutableMapOf<String, ArtistStats>()
        for (track in tracks) {
            val albumArtist = track.albumArtist?.takeIf { it.isNotBlank() } ?: track.artist
            val albumKey = AlbumKey(track.album, albumArtist)
            val albumEntry = albumStats.getOrPut(albumKey) { AlbumStats() }
            albumEntry.trackCount++
            albumEntry.totalDurationMs += track.durationMs
            albumEntry.dateAdded = minOf(albumEntry.dateAdded, track.dateAdded)

            val artistEntry = artistStats.getOrPut(track.artist) { ArtistStats() }
            artistEntry.trackCount++
            artistEntry.albumNames.add(track.album)
            artistEntry.dateAdded = minOf(artistEntry.dateAdded, track.dateAdded)
        }

        val albums = albumStats.map { (key, stats) ->
            LocalAlbumEntity(
                name = key.name,
                artist = key.artist,
                trackCount = stats.trackCount,
                totalDurationMs = stats.totalDurationMs,
                dateAdded = stats.dateAdded
            )
        }

        val artists = artistStats.map { (name, stats) ->
            LocalArtistEntity(
                name = name,
                albumCount = stats.albumNames.size,
                trackCount = stats.trackCount,
                dateAdded = stats.dateAdded
            )
        }

        return albums to artists
    }

    private fun flushPendingTracks() {
        if (pendingTracks.isEmpty()) return
        stagedTracks.addAll(pendingTracks)
        pendingTracks.clear()
    }

    private suspend fun persistScanResults(
        tracks: List<LocalTrackEntity>,
        albums: List<LocalAlbumEntity>,
        artists: List<LocalArtistEntity>
    ) {
        localMediaDatabase.withTransaction {
            localTrackDao.deleteAllTracks()
            localAlbumDao.deleteAllAlbums()
            localArtistDao.deleteAllArtists()

            if (tracks.isNotEmpty()) {
                tracks.chunked(TRACK_BATCH_SIZE).forEach { batch ->
                    localTrackDao.upsertTracks(batch)
                }
            }
            for (album in albums) {
                localAlbumDao.upsertAlbum(album)
            }
            for (artist in artists) {
                localArtistDao.upsertArtist(artist)
            }
        }
    }

    private fun readMetadata(uri: Uri): TrackMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            TrackMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "",
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "",
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "",
                albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                trackNumber = parseTrackNumber(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ),
                durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
            )
        } finally {
            retriever.release()
        }
    }

    private fun parseTrackNumber(rawValue: String?): Int {
        val raw = rawValue?.trim().orEmpty()
        if (raw.isBlank()) return 0
        val primary = raw.split("/", limit = 2).firstOrNull().orEmpty()
        return primary.toIntOrNull() ?: 0
    }

    private fun failureResult(message: String, throwable: Throwable? = null): Result<ScanResult> {
        Logger.e(TAG, message, throwable)
        _scanProgress.value = ScanProgress.Error(message)
        return Result.failure(throwable ?: IllegalArgumentException(message))
    }

    private data class TrackMetadata(
        val title: String,
        val artist: String,
        val album: String,
        val albumArtist: String?,
        val trackNumber: Int,
        val durationMs: Long
    )

    private data class AlbumKey(val name: String, val artist: String)

    private data class AlbumStats(
        var trackCount: Int = 0,
        var totalDurationMs: Long = 0L,
        var dateAdded: Long = Long.MAX_VALUE
    )

    private data class ArtistStats(
        val albumNames: MutableSet<String> = mutableSetOf(),
        var trackCount: Int = 0,
        var dateAdded: Long = Long.MAX_VALUE
    )

    data class ScanResult(
        val filesScanned: Int,
        val tracksAdded: Int,
        val albumsAdded: Int,
        val artistsAdded: Int
    )

    sealed class ScanProgress {
        data object Idle : ScanProgress()
        data class Scanning(val current: Int, val total: Int) : ScanProgress()
        data class Complete(val result: ScanResult) : ScanProgress()
        data class Error(val message: String) : ScanProgress()
    }

    private companion object {
        private const val TAG = "LocalMediaScanner"
        private const val TRACK_BATCH_SIZE = 50
        private const val PROGRESS_UPDATE_INTERVAL = 10
        private const val DEFAULT_AUDIO_MIME_TYPE = "audio/*"
        private val AUDIO_EXTENSION_MIME_TYPES = mapOf(
            "mp3" to "audio/mpeg",
            "m4a" to "audio/mp4",
            "flac" to "audio/flac",
            "wav" to "audio/wav",
            "ogg" to "audio/ogg",
            "opus" to "audio/opus"
        )
    }
}
