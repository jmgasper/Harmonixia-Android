package com.harmonixia.android.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.domain.manager.DownloadManager
import com.harmonixia.android.domain.model.DownloadProgress
import com.harmonixia.android.domain.model.DownloadStatus
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.model.downloadId
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.service.playback.PlaybackStateManager
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.PlayerSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val downloadRepository: DownloadRepository,
    private val musicAssistantRepository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager
) : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val overallProgress: StateFlow<DownloadProgress> = downloadManager.getOverallProgress()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DownloadProgress(trackId = "overall_downloads")
        )

    private val activeDownloadsFlow: Flow<List<DownloadItemUi>> = combine(
        downloadRepository.getAllPendingDownloads(),
        downloadRepository.getAllInProgressDownloads()
    ) { pending, inProgress ->
        pending + inProgress
    }.flatMapLatest { downloads ->
        if (downloads.isEmpty()) {
            flowOf(emptyList())
        } else {
            val progressFlows = downloads.map { download ->
                downloadRepository.getDownloadProgress(download.track.downloadId)
                    .map { progress -> progress ?: DownloadProgress(trackId = download.track.downloadId) }
            }
            combine(progressFlows) { progresses ->
                downloads.mapIndexed { index, download ->
                    DownloadItemUi(download = download, progress = progresses[index])
                }
            }
        }
    }

    val uiState: StateFlow<DownloadsUiState> = combine(
        activeDownloadsFlow,
        downloadRepository.getDownloadedPlaylists(),
        downloadRepository.getDownloadedAlbums(),
        downloadRepository.getDownloadedTracks(),
        overallProgress
    ) { activeDownloads, playlists, albums, tracks, overallProgress ->
        if (
            activeDownloads.isEmpty() &&
            playlists.isEmpty() &&
            albums.isEmpty() &&
            tracks.isEmpty()
        ) {
            DownloadsUiState.Empty
        } else {
            DownloadsUiState.Success(
                overallProgress = overallProgress,
                inProgressDownloads = activeDownloads,
                downloadedPlaylists = playlists,
                downloadedAlbums = albums,
                downloadedTracks = tracks
            )
        }
    }
        .catch { error ->
            emit(DownloadsUiState.Error(error.message ?: "Failed to load downloads"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState.Loading)

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(400)
            _isRefreshing.value = false
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadManager.cancelAllDownloads()
            downloadRepository.deleteAllDownloadedMedia()
            downloadRepository.deleteAllDownloads()
        }
    }

    fun cancelDownload(trackId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(trackId)
        }
    }

    fun pauseDownload(trackId: String) {
        viewModelScope.launch {
            downloadManager.pauseDownload(trackId)
        }
    }

    fun resumeDownload(trackId: String) {
        viewModelScope.launch {
            downloadManager.resumeDownload(trackId)
        }
    }

    fun getTrackDownloadStatus(trackId: String): Flow<DownloadStatus?> {
        return downloadRepository.getDownloadStatus(trackId)
    }

    fun playTrack(track: Track) {
        if (track.uri.isBlank()) return
        viewModelScope.launch {
            val players = musicAssistantRepository.fetchPlayers()
                .getOrElse {
                    Logger.w(TAG, "Failed to fetch players", it)
                    return@launch
                }
            val player = PlayerSelection.selectLocalPlayer(players)
            if (player == null) {
                Logger.w(TAG, "No local playback device available")
                return@launch
            }
            val queue = musicAssistantRepository.getActiveQueue(player.playerId)
                .getOrElse {
                    Logger.w(TAG, "Failed to fetch active queue", it)
                    return@launch
                } ?: run {
                Logger.w(TAG, "No active queue available")
                return@launch
            }
            playbackStateManager.notifyUserInitiatedPlayback()
            musicAssistantRepository.playMedia(queue.queueId, listOf(track.uri), QueueOption.REPLACE)
                .onFailure { Logger.w(TAG, "Failed to play track ${track.downloadId}", it) }
        }
    }
}

internal data class SpeedDisplay(
    val value: Double,
    val unit: SpeedUnit
)

internal enum class SpeedUnit {
    KBPS,
    MBPS
}

internal fun formatDownloadSpeed(speedBps: Long): SpeedDisplay? {
    if (speedBps <= 0L) return null
    val kbps = speedBps / 1000.0
    return if (kbps < 1000.0) {
        SpeedDisplay(kbps, SpeedUnit.KBPS)
    } else {
        SpeedDisplay(kbps / 1000.0, SpeedUnit.MBPS)
    }
}

private const val TAG = "DownloadsViewModel"
