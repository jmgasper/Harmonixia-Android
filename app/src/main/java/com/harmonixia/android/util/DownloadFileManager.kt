package com.harmonixia.android.util

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getDownloadDirectory(): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    fun getCoverArtDirectory(): File {
        val coverDir = File(getDownloadDirectory(), COVER_ART_DIR)
        if (!coverDir.exists()) {
            coverDir.mkdirs()
        }
        return coverDir
    }

    fun generateTrackFilePath(trackId: String, provider: String, extension: String): String {
        val safeTrackId = sanitizeSegment(trackId)
        val safeProvider = sanitizeSegment(provider)
        val safeExtension = sanitizeExtension(extension)
        val filename = "${safeTrackId}_${safeProvider}.$safeExtension"
        return File(getDownloadDirectory(), filename).absolutePath
    }

    fun generateCoverArtPath(itemId: String, provider: String): String {
        val safeItemId = sanitizeSegment(itemId)
        val safeProvider = sanitizeSegment(provider)
        val filename = "${safeItemId}_${safeProvider}.jpg"
        return File(getCoverArtDirectory(), filename).absolutePath
    }

    suspend fun saveFile(inputStream: InputStream, filePath: String): Result<Long> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val file = File(filePath)
                file.parentFile?.mkdirs()
                inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                file.length()
            }
        }
    }

    suspend fun deleteFile(filePath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (filePath.isBlank()) return@runCatching
                val file = File(filePath)
                if (file.exists() && !file.delete()) {
                    error("Failed to delete file: $filePath")
                }
            }
        }
    }

    suspend fun getFileSize(filePath: String): Long {
        return withContext(Dispatchers.IO) {
            if (filePath.isBlank()) return@withContext 0L
            val file = File(filePath)
            if (file.exists()) file.length() else 0L
        }
    }

    fun fileExists(filePath: String): Boolean {
        if (filePath.isBlank()) return false
        return File(filePath).exists()
    }

    private fun sanitizeSegment(value: String): String {
        return value.replace(SEGMENT_REGEX, "_")
    }

    private fun sanitizeExtension(value: String): String {
        val trimmed = value.trim().trimStart('.')
        return if (trimmed.isBlank()) DEFAULT_EXTENSION else trimmed
    }

    private companion object {
        private val SEGMENT_REGEX = Regex("[^A-Za-z0-9._-]")
        private const val DEFAULT_EXTENSION = "bin"
        private const val COVER_ART_DIR = "cover_art"
    }
}
