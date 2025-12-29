package com.harmonixia.android.domain.manager

import androidx.lifecycle.Observer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.harmonixia.android.data.local.DownloadSettingsDataStore
import com.harmonixia.android.domain.model.DownloadProgress
import com.harmonixia.android.domain.model.DownloadStatus
import com.harmonixia.android.domain.model.DownloadedTrack
import com.harmonixia.android.domain.model.downloadId
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.work.DownloadWorker
import com.harmonixia.android.work.KEY_TRACK_DOWNLOAD_ID
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class DownloadManager(
    private val workManager: WorkManager,
    private val downloadRepository: DownloadRepository,
    private val downloadSettingsDataStore: DownloadSettingsDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    fun start() {
        if (monitorJob != null) return
        monitorJob = scope.launch {
            combine(
                downloadRepository.getAllPendingDownloads(),
                downloadRepository.getAllInProgressDownloads(),
                downloadSettingsDataStore.getMaxConcurrentDownloads(),
                downloadSettingsDataStore.getDownloadOverWifiOnly()
            ) { pending, _, maxConcurrent, wifiOnly ->
                Triple(pending, maxConcurrent, wifiOnly)
            }.collect { (pending, maxConcurrent, wifiOnly) ->
                enqueuePendingDownloads(pending, maxConcurrent, wifiOnly)
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    suspend fun startDownload(trackId: String) {
        downloadRepository.updateDownloadStatus(trackId, DownloadStatus.PENDING)
    }

    suspend fun pauseDownload(trackId: String) {
        cancelDownload(trackId)
    }

    suspend fun resumeDownload(trackId: String) {
        downloadRepository.updateDownloadStatus(trackId, DownloadStatus.PENDING)
    }

    suspend fun cancelDownload(trackId: String) {
        workManager.cancelUniqueWork("$WORK_NAME_PREFIX$trackId")
        downloadRepository.updateDownloadStatus(trackId, DownloadStatus.PAUSED)
    }

    suspend fun cancelAllDownloads() {
        workManager.cancelAllWorkByTag(TAG_DOWNLOAD_WORKER)
        val pendingDownloads = downloadRepository.getAllPendingDownloads().first()
        val activeDownloads = downloadRepository.getAllInProgressDownloads().first()
        val downloadsToPause = pendingDownloads + activeDownloads
        for (download in downloadsToPause) {
            downloadRepository.updateDownloadStatus(download.track.downloadId, DownloadStatus.PAUSED)
        }
    }

    fun getActiveDownloadCount(): Flow<Int> {
        return workInfosByTagFlow(TAG_DOWNLOAD_WORKER)
            .map { workInfos -> workInfos.count { !it.state.isFinished } }
    }

    fun getOverallProgress(): Flow<DownloadProgress> {
        return downloadRepository.getAllInProgressDownloads().flatMapLatest { downloads ->
            if (downloads.isEmpty()) {
                return@flatMapLatest flowOf(DownloadProgress(trackId = OVERALL_TRACK_ID))
            }
            val progressFlows = downloads.map { download ->
                downloadRepository.getDownloadProgress(download.track.downloadId)
            }
            combine(progressFlows) { progresses ->
                val progressList = progresses.filterNotNull()
                val totalBytes = progressList.sumOf { it.totalBytes }
                val downloadedBytes = progressList.sumOf { it.bytesDownloaded }
                val speedBps = progressList.sumOf { it.downloadSpeedBps }
                val progressPercent = if (totalBytes > 0L) {
                    ((downloadedBytes * 100L) / totalBytes).toInt()
                } else {
                    0
                }
                DownloadProgress(
                    trackId = OVERALL_TRACK_ID,
                    bytesDownloaded = downloadedBytes,
                    totalBytes = totalBytes,
                    downloadSpeedBps = speedBps,
                    progress = progressPercent,
                    timestampMillis = System.currentTimeMillis()
                )
            }
        }
    }

    private fun workInfosByTagFlow(tag: String): Flow<List<WorkInfo>> = callbackFlow {
        val liveData = workManager.getWorkInfosByTagLiveData(tag)
        val observer = Observer<List<WorkInfo>> { infos ->
            trySend(infos).isSuccess
        }
        liveData.observeForever(observer)
        awaitClose { liveData.removeObserver(observer) }
    }

    private suspend fun enqueuePendingDownloads(
        pending: List<DownloadedTrack>,
        maxConcurrent: Int,
        wifiOnly: Boolean
    ) {
        if (pending.isEmpty()) return
        val activeCount = getActiveWorkerCount()
        val availableSlots = (maxConcurrent - activeCount).coerceAtLeast(0)
        if (availableSlots <= 0) return
        pending.take(availableSlots).forEach { download ->
            enqueueDownload(download.track.downloadId, wifiOnly)
        }
    }

    private suspend fun getActiveWorkerCount(): Int {
        return withContext(Dispatchers.IO) {
            val workInfos = runCatching {
                workManager.getWorkInfosByTag(TAG_DOWNLOAD_WORKER).get()
            }.getOrDefault(emptyList())
            workInfos.count { !it.state.isFinished }
        }
    }

    private fun enqueueDownload(trackId: String, wifiOnly: Boolean) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(KEY_TRACK_DOWNLOAD_ID to trackId))
            .setConstraints(buildConstraints(wifiOnly))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                INITIAL_BACKOFF_DELAY_MS,
                TimeUnit.MILLISECONDS
            )
            .addTag(TAG_DOWNLOAD_WORKER)
            .build()
        workManager.enqueueUniqueWork(
            "$WORK_NAME_PREFIX$trackId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun buildConstraints(wifiOnly: Boolean): Constraints {
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        return Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(false)
            .build()
    }

    private companion object {
        private const val OVERALL_TRACK_ID = "overall_downloads"
        const val TAG_DOWNLOAD_WORKER = "download_worker"
        private const val WORK_NAME_PREFIX = "download_track_"
        private const val INITIAL_BACKOFF_DELAY_MS = 30_000L
    }
}
