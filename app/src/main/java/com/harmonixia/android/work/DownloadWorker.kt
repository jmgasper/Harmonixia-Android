package com.harmonixia.android.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.domain.model.DownloadProgress
import com.harmonixia.android.domain.model.DownloadStatus
import com.harmonixia.android.domain.model.DownloadedTrack
import com.harmonixia.android.domain.model.downloadId
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.util.DownloadFileManager
import com.harmonixia.android.util.Logger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(KEY_TRACK_DOWNLOAD_ID)?.trim()
        if (downloadId.isNullOrBlank()) return Result.failure()

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DownloadWorkerEntryPoint::class.java
        )
        val repository = entryPoint.downloadRepository()
        val fileManager = entryPoint.downloadFileManager()
        val okHttpClient = entryPoint.okHttpClient()
        val settingsDataStore = entryPoint.settingsDataStore()

        val download = repository.getDownloadedTrack(downloadId) ?: return Result.failure()
        val authToken = settingsDataStore.getAuthToken().first().trim()
        val serverUrl = settingsDataStore.getServerUrl().first().trim()
        val trackUrl = resolveUrl(serverUrl, download.track.uri)

        repository.updateDownloadStatus(downloadId, DownloadStatus.IN_PROGRESS)

        return try {
            val fileSize = downloadAudio(
                download = download,
                url = trackUrl,
                authToken = authToken,
                okHttpClient = okHttpClient,
                fileManager = fileManager,
                repository = repository
            )
            if (!download.track.imageUrl.isNullOrBlank()) {
                downloadCoverArt(
                    download = download,
                    serverUrl = serverUrl,
                    authToken = authToken,
                    okHttpClient = okHttpClient,
                    fileManager = fileManager
                )
            }
            repository.updateDownloadCompleted(downloadId, fileSize)
            Result.success()
        } catch (error: HttpStatusException) {
            handleHttpFailure(downloadId, download, repository, fileManager, error.statusCode)
        } catch (error: IOException) {
            handleFailure(downloadId, download, repository, fileManager)
        } catch (error: CancellationException) {
            Logger.w(TAG, "Download cancelled for $downloadId")
            repository.updateDownloadStatus(downloadId, DownloadStatus.PAUSED)
            cleanupPartialFiles(download, fileManager)
            throw error
        } catch (error: Exception) {
            Logger.w(TAG, "Download failed for $downloadId", error)
            repository.updateDownloadStatus(downloadId, DownloadStatus.FAILED)
            cleanupPartialFiles(download, fileManager)
            Result.failure()
        }
    }

    private suspend fun downloadAudio(
        download: DownloadedTrack,
        url: String,
        authToken: String,
        okHttpClient: OkHttpClient,
        fileManager: DownloadFileManager,
        repository: DownloadRepository
    ): Long {
        return withContext(Dispatchers.IO) {
            val request = buildRequest(url, authToken)
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw HttpStatusException(response.code)
                }
                val body = response.body ?: throw IOException("Empty response body")
                val totalBytes = body.contentLength().coerceAtLeast(0L)
                repository.updateDownloadProgress(
                    download.track.downloadId,
                    DownloadProgress(trackId = download.track.downloadId, totalBytes = totalBytes)
                )
                val progressStream = ProgressInputStream(
                    delegate = body.byteStream(),
                    totalBytes = totalBytes,
                    updateIntervalMs = PROGRESS_UPDATE_INTERVAL_MS
                ) { bytesDownloaded, totalSize, speedBps, progressPercent, timestamp ->
                    val progress = DownloadProgress(
                        trackId = download.track.downloadId,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalSize,
                        downloadSpeedBps = speedBps,
                        progress = progressPercent,
                        timestampMillis = timestamp
                    )
                    runBlocking {
                        repository.updateDownloadProgress(download.track.downloadId, progress)
                    }
                }
                val fileSize = fileManager.saveFile(progressStream, download.localFilePath).getOrThrow()
                repository.updateDownloadProgress(
                    download.track.downloadId,
                    DownloadProgress(
                        trackId = download.track.downloadId,
                        bytesDownloaded = fileSize,
                        totalBytes = totalBytes,
                        downloadSpeedBps = 0L,
                        progress = 100
                    )
                )
                fileSize
            }
        }
    }

    private suspend fun downloadCoverArt(
        download: DownloadedTrack,
        serverUrl: String,
        authToken: String,
        okHttpClient: OkHttpClient,
        fileManager: DownloadFileManager
    ) {
        val imageUrl = download.track.imageUrl?.trim().orEmpty()
        val coverArtPath = download.coverArtPath?.trim().orEmpty()
        if (imageUrl.isBlank() || coverArtPath.isBlank()) return
        val resolvedUrl = resolveUrl(serverUrl, imageUrl)
        val request = buildRequest(resolvedUrl, authToken)
        val result = runCatching {
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Logger.w(TAG, "Cover art download failed with ${response.code}")
                        return@use
                    }
                    val body = response.body ?: return@use
                    fileManager.saveFile(body.byteStream(), coverArtPath).getOrThrow()
                }
            }
        }
        if (result.isFailure) {
            Logger.w(TAG, "Cover art download failed", result.exceptionOrNull())
            fileManager.deleteFile(coverArtPath)
        }
    }

    private suspend fun handleFailure(
        downloadId: String,
        download: DownloadedTrack,
        repository: DownloadRepository,
        fileManager: DownloadFileManager
    ): Result {
        Logger.w(TAG, "Download failed for $downloadId, attempt ${runAttemptCount + 1}")
        cleanupPartialFiles(download, fileManager)
        return if (runAttemptCount >= MAX_RETRY_ATTEMPTS - 1) {
            repository.updateDownloadStatus(downloadId, DownloadStatus.FAILED)
            Result.failure()
        } else {
            Result.retry()
        }
    }

    private suspend fun handleHttpFailure(
        downloadId: String,
        download: DownloadedTrack,
        repository: DownloadRepository,
        fileManager: DownloadFileManager,
        statusCode: Int
    ): Result {
        cleanupPartialFiles(download, fileManager)
        return when {
            statusCode == 401 || statusCode == 403 || statusCode == 404 -> {
                repository.updateDownloadStatus(downloadId, DownloadStatus.FAILED)
                Result.failure()
            }
            statusCode in 500..599 -> handleFailure(downloadId, download, repository, fileManager)
            else -> {
                repository.updateDownloadStatus(downloadId, DownloadStatus.FAILED)
                Result.failure()
            }
        }
    }

    private suspend fun cleanupPartialFiles(
        download: DownloadedTrack,
        fileManager: DownloadFileManager
    ) {
        fileManager.deleteFile(download.localFilePath)
        val coverArtPath = download.coverArtPath?.trim().orEmpty()
        if (coverArtPath.isNotBlank()) {
            fileManager.deleteFile(coverArtPath)
        }
    }

    private fun buildRequest(url: String, authToken: String): Request {
        val builder = Request.Builder().url(url)
        if (authToken.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer $authToken")
        }
        return builder.build()
    }

    private fun resolveUrl(serverUrl: String, rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.contains("://")) return trimmed
        if (serverUrl.isBlank()) return trimmed
        val base = serverUrl.trimEnd('/')
        val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return "$base$path"
    }

    private companion object {
        private const val TAG = "DownloadWorker"
    }
}

private class ProgressInputStream(
    private val delegate: InputStream,
    private val totalBytes: Long,
    private val updateIntervalMs: Long,
    private val onProgress: (
        bytesDownloaded: Long,
        totalBytes: Long,
        speedBps: Long,
        progressPercent: Int,
        timestampMillis: Long
    ) -> Unit
) : InputStream() {
    private var bytesRead: Long = 0
    private var lastUpdateTime: Long = System.currentTimeMillis()
    private var lastUpdateBytes: Long = 0

    override fun read(): Int {
        val byte = delegate.read()
        if (byte >= 0) {
            bytesRead += 1
            maybeReport()
        }
        return byte
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = delegate.read(buffer, offset, length)
        if (read > 0) {
            bytesRead += read
            maybeReport()
        }
        return read
    }

    override fun close() {
        delegate.close()
    }

    private fun maybeReport() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastUpdateTime
        if (elapsed < updateIntervalMs) return
        val deltaBytes = bytesRead - lastUpdateBytes
        val speedBps = if (elapsed > 0) (deltaBytes * 1000L) / elapsed else 0L
        val progressPercent = if (totalBytes > 0L) {
            ((bytesRead * 100L) / totalBytes).toInt()
        } else {
            0
        }
        onProgress(bytesRead, totalBytes, speedBps, progressPercent, now)
        lastUpdateTime = now
        lastUpdateBytes = bytesRead
    }
}

private class HttpStatusException(val statusCode: Int) : IOException()

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadWorkerEntryPoint {
    fun downloadRepository(): DownloadRepository
    fun downloadFileManager(): DownloadFileManager
    fun okHttpClient(): OkHttpClient
    fun settingsDataStore(): SettingsDataStore
}

const val KEY_TRACK_DOWNLOAD_ID = "track_download_id"
const val TAG_DOWNLOAD_WORKER = "download_worker"
const val PROGRESS_UPDATE_INTERVAL_MS = 500L
const val MAX_RETRY_ATTEMPTS = 3
const val INITIAL_BACKOFF_DELAY_MS = 30_000L
