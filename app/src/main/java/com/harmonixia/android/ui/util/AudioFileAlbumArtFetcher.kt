package com.harmonixia.android.ui.util

import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.pathSegments
import coil3.request.Options
import coil3.toAndroidUri
import java.util.Locale
import okio.Buffer
import okio.FileSystem

class AudioFileAlbumArtFetcher(
    private val data: Uri,
    private val options: Options,
    private val imageLoader: ImageLoader
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val diskCacheKey = options.diskCacheKey ?: data.toString()
        val diskCache = imageLoader.diskCache
        if (options.diskCachePolicy.readEnabled && diskCache != null) {
            diskCache.openSnapshot(diskCacheKey)?.let { snapshot ->
                return snapshot.toFetchResult(diskCacheKey, diskCache.fileSystem)
            }
        }

        val embeddedPicture = loadEmbeddedPicture() ?: return null
        if (embeddedPicture.isEmpty()) return null

        if (options.diskCachePolicy.writeEnabled && diskCache != null) {
            val editor = diskCache.openEditor(diskCacheKey)
            if (editor != null) {
                val fileSystem = diskCache.fileSystem
                try {
                    fileSystem.write(editor.metadata) { }
                    fileSystem.write(editor.data) { write(embeddedPicture) }
                    val snapshot = editor.commitAndOpenSnapshot()
                    if (snapshot != null) {
                        return snapshot.toFetchResult(diskCacheKey, fileSystem)
                    }
                } catch (_: Exception) {
                    editor.abort()
                }
            }
        }

        val buffer = Buffer().write(embeddedPicture)
        return SourceFetchResult(
            source = ImageSource(buffer, options.fileSystem),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    private fun loadEmbeddedPicture(): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(options.context, data.toAndroidUri())
            retriever.embeddedPicture
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun DiskCache.Snapshot.toFetchResult(
        diskCacheKey: String,
        fileSystem: FileSystem
    ): SourceFetchResult {
        return SourceFetchResult(
            source = ImageSource(
                file = data,
                fileSystem = fileSystem,
                diskCacheKey = diskCacheKey,
                closeable = this
            ),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (isAudioFileUri(data, options)) {
                AudioFileAlbumArtFetcher(data, options, imageLoader)
            } else {
                null
            }
        }

        private fun isAudioFileUri(data: Uri, options: Options): Boolean {
            val scheme = data.scheme
            val isFileOrContent = scheme == null ||
                scheme == SCHEME_FILE ||
                scheme == ContentResolver.SCHEME_CONTENT
            if (!isFileOrContent) return false

            val mimeType = if (scheme == ContentResolver.SCHEME_CONTENT) {
                options.context.contentResolver.getType(data.toAndroidUri())
            } else {
                null
            }
            if (mimeType?.lowercase(Locale.US)?.startsWith("audio/") == true) {
                return true
            }
            val extension = data.pathSegments.lastOrNull()
                ?.substringAfterLast('.', missingDelimiterValue = "")
                ?.takeIf { it.isNotBlank() }
                ?.lowercase(Locale.US)
            return extension != null && AUDIO_EXTENSIONS.contains(extension)
        }
    }

    private companion object {
        private const val SCHEME_FILE = "file"
        private val AUDIO_EXTENSIONS = setOf(
            "mp3",
            "m4a",
            "flac",
            "wav",
            "ogg",
            "opus"
        )
    }
}
