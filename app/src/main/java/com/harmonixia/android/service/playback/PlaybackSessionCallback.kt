package com.harmonixia.android.service.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.PerformanceMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@UnstableApi
class PlaybackSessionCallback(
    private val player: Player,
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager,
    private val queueManager: QueueManager,
    private val performanceMonitor: PerformanceMonitor,
    private val scope: CoroutineScope
) : MediaSession.Callback {

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return
            markPlaybackRequestedForMediaItem(mediaItem)
            val queueId = playbackStateManager.currentQueueId ?: return
            scope.launch {
                repository.nextTrack(queueId)
                    .onFailure { Logger.w(TAG, "Auto-advance command failed", it) }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason != Player.DISCONTINUITY_REASON_SEEK) return
            val queueId = playbackStateManager.currentQueueId ?: return
            val positionSeconds = (player.currentPosition / 1000L).toInt()
            scope.launch {
                repository.seekTo(queueId, positionSeconds)
                    .onFailure { Logger.w(TAG, "Seek command failed", it) }
            }
        }
    }

    init {
        player.addListener(playerListener)
    }

    override fun onPlayerCommandRequest(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        @Player.Command playerCommand: Int
    ): Int {
        when (playerCommand) {
            Player.COMMAND_PLAY_PAUSE -> {
                if (player.isPlaying) handlePause() else handlePlay()
            }
            Player.COMMAND_STOP -> handleStop()
            Player.COMMAND_SEEK_TO_NEXT -> handleNext()
            Player.COMMAND_SEEK_TO_PREVIOUS -> handlePrevious()
        }
        return SessionResult.RESULT_SUCCESS
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        playbackStateManager.notifyUserInitiatedPlayback()
        markPlaybackRequestedForMediaItem(mediaItems.firstOrNull())
        val queueId = playbackStateManager.currentQueueId
        if (queueId != null) {
            val uris = mediaItems.mapNotNull { it.localConfiguration?.uri?.toString() }
            scope.launch {
                repository.playMedia(queueId, uris, QueueOption.ADD)
                    .onFailure { Logger.w(TAG, "Add to queue failed", it) }
            }
        }
        queueManager.addMediaItems(mediaItems)
        return Futures.immediateFuture(mediaItems)
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        playbackStateManager.notifyUserInitiatedPlayback()
        markPlaybackRequestedForStartItem(mediaItems, startIndex)
        val queueId = playbackStateManager.currentQueueId
        if (queueId != null) {
            val uris = mediaItems.mapNotNull { it.localConfiguration?.uri?.toString() }
            scope.launch {
                repository.playMedia(queueId, uris, QueueOption.REPLACE)
                    .onFailure { Logger.w(TAG, "Replace queue failed", it) }
            }
        }
        queueManager.replaceQueue(mediaItems, startIndex, startPositionMs)
        val result = MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
        return Futures.immediateFuture(result)
    }

    private fun markPlaybackRequestedForMediaItem(mediaItem: MediaItem?) {
        performanceMonitor.markPlaybackRequested(mediaItem?.mediaId.orEmpty())
    }

    private fun markPlaybackRequestedForStartItem(mediaItems: List<MediaItem>, startIndex: Int) {
        if (mediaItems.isEmpty()) return
        val safeIndex = if (startIndex in mediaItems.indices) startIndex else 0
        markPlaybackRequestedForMediaItem(mediaItems[safeIndex])
    }

    private fun markPlaybackRequestedForIndex(index: Int) {
        if (index == C.INDEX_UNSET) return
        if (index < 0 || index >= player.mediaItemCount) return
        markPlaybackRequestedForMediaItem(player.getMediaItemAt(index))
    }

    private fun handlePlay() {
        playbackStateManager.notifyUserInitiatedPlayback()
        markPlaybackRequestedForMediaItem(player.currentMediaItem)
        player.play()
        val queueId = playbackStateManager.currentQueueId ?: return
        scope.launch {
            repository.resumeQueue(queueId)
                .onFailure { Logger.w(TAG, "Resume command failed", it) }
        }
    }

    private fun handlePause() {
        player.pause()
        val queueId = playbackStateManager.currentQueueId ?: return
        scope.launch {
            repository.pauseQueue(queueId)
                .onFailure { Logger.w(TAG, "Pause command failed", it) }
        }
    }

    private fun handleStop() {
        player.stop()
        performanceMonitor.clearPlaybackRequests()
        val queueId = playbackStateManager.currentQueueId ?: return
        scope.launch {
            repository.pauseQueue(queueId)
                .onFailure { Logger.w(TAG, "Stop command failed", it) }
        }
    }

    private fun handleNext() {
        markPlaybackRequestedForIndex(player.nextMediaItemIndex)
        player.seekToNext()
        val queueId = playbackStateManager.currentQueueId ?: return
        scope.launch {
            repository.nextTrack(queueId)
                .onFailure { Logger.w(TAG, "Next command failed", it) }
        }
    }

    private fun handlePrevious() {
        markPlaybackRequestedForIndex(player.previousMediaItemIndex)
        player.seekToPrevious()
        val queueId = playbackStateManager.currentQueueId ?: return
        scope.launch {
            repository.previousTrack(queueId)
                .onFailure { Logger.w(TAG, "Previous command failed", it) }
        }
    }

    companion object {
        private const val TAG = "PlaybackSessionCallback"
    }
}
