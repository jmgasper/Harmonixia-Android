package com.harmonixia.android.service.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.EXTRA_PARENT_MEDIA_ID
import com.harmonixia.android.util.EXTRA_STREAM_URI
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.PerformanceMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@UnstableApi
class PlaybackSessionCallback(
    private val player: Player,
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager,
    private val queueManager: QueueManager,
    private val mediaLibraryBrowser: MediaLibraryBrowser,
    private val performanceMonitor: PerformanceMonitor,
    private val scope: CoroutineScope
) : MediaLibrarySession.Callback {

    private val playerListener = object : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason != Player.DISCONTINUITY_REASON_SEEK) return
            if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex) return
            if (playbackStateManager.consumeSyncSeekSuppression()) return
            if (queueManager.isLocalQueueActive()) return
            val queueId = playbackStateManager.currentQueueId ?: return
            val positionSeconds = (player.currentPosition / 1000L).toInt()
            val timestamp = System.currentTimeMillis()
            Logger.d(
                TAG,
                "Requesting seek queueId=$queueId position=$positionSeconds at $timestamp"
            )
            scope.launch {
                repository.seekTo(queueId, positionSeconds)
                    .onFailure { Logger.w(TAG, "Seek command failed", it) }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            handleShuffleModeChange(shuffleModeEnabled)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            handleRepeatModeChange(repeatMode)
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

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val future = SettableFuture.create<LibraryResult<MediaItem>>()
        scope.launch(Dispatchers.IO) {
            val root = runCatching { mediaLibraryBrowser.getLibraryRoot(params?.extras) }
                .getOrElse {
                    Logger.w(TAG, "Failed to load library root", it)
                    val metadata = MediaMetadata.Builder()
                        .setTitle("Harmonixia")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                    MediaItem.Builder()
                        .setMediaId("root")
                        .setMediaMetadata(metadata)
                        .build()
                }
            future.set(LibraryResult.ofItem(root, params))
        }
        return future
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        scope.launch(Dispatchers.IO) {
            val children = runCatching {
                mediaLibraryBrowser.getChildren(parentId, page, pageSize)
            }.getOrElse {
                Logger.w(TAG, "Failed to load children for $parentId", it)
                emptyList()
            }
            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(children), params))
        }
        return future
    }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        val future = SettableFuture.create<LibraryResult<Void>>()
        scope.launch(Dispatchers.IO) {
            val results = runCatching { mediaLibraryBrowser.getSearchResults(query) }
                .getOrElse {
                    Logger.w(TAG, "Failed to search library for $query", it)
                    null
                }
            val count = results?.let { it.albums.size + it.artists.size + it.playlists.size + it.tracks.size } ?: 0
            session.notifySearchResultChanged(browser, query, count, params)
            future.set(LibraryResult.ofVoid(params))
        }
        return future
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        scope.launch(Dispatchers.IO) {
            val items = runCatching {
                mediaLibraryBrowser.search(query, page, pageSize)
            }.getOrElse {
                Logger.w(TAG, "Failed to load search results for $query", it)
                emptyList()
            }
            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }
        return future
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        val resolvedItems = resolveMediaItems(mediaItems)
        val isLocalQueue = queueManager.areMediaItemsLocal(resolvedItems)
        playbackStateManager.notifyUserInitiatedPlayback()
        markPlaybackRequestedForMediaItem(resolvedItems.firstOrNull())
        scope.launch(Dispatchers.IO) {
            if (isLocalQueue) {
                Logger.d(TAG, "Skipping remote add for local queue")
                return@launch
            }
            playbackStateManager.reconnectLocalPlayerIfUnavailable()
            val queueId = playbackStateManager.currentQueueId ?: awaitQueueId()
            if (queueId.isNullOrBlank()) {
                Logger.w(TAG, "No active queue available for add request")
                return@launch
            }
            val uris = resolvedItems.mapNotNull { it.streamUri() }
            if (uris.isEmpty()) {
                Logger.w(TAG, "No stream URIs resolved for add request")
                return@launch
            }
            Logger.d(
                TAG,
                "Requesting playMedia queueId=$queueId option=ADD count=${uris.size}"
            )
            repository.playMedia(queueId, uris, QueueOption.ADD)
                .onFailure { Logger.w(TAG, "Add to queue failed", it) }
        }
        queueManager.addMediaItems(resolvedItems)
        return Futures.immediateFuture(resolvedItems)
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        scope.launch {
            val resolvedItems = resolveMediaItems(mediaItems)
            val safeIndex = if (startIndex in resolvedItems.indices) startIndex else 0
            val startItem = resolvedItems.getOrNull(safeIndex)
            val parentMediaId = startItem?.mediaMetadata?.extras?.getString(EXTRA_PARENT_MEDIA_ID)
            val playlistUri = parentMediaId?.let { mediaLibraryBrowser.resolvePlaylistUri(it) }
            val startItemUri = startItem?.streamUri()
            val usePlaylistUri = !playlistUri.isNullOrBlank() && !startItemUri.isNullOrBlank()
            val (queueItems, queueStartIndex) = if (usePlaylistUri) {
                resolvedItems to safeIndex
            } else {
                runCatching {
                    resolveRemainingQueue(resolvedItems, startIndex)
                }.getOrElse { error ->
                    Logger.w(TAG, "Failed to build queue from parent media", error)
                    resolvedItems to safeIndex
                }
            }
            val isLocalQueue = queueManager.areMediaItemsLocal(queueItems)
            playbackStateManager.notifyUserInitiatedPlayback()
            markPlaybackRequestedForStartItem(queueItems, queueStartIndex)
            if (queueStartIndex > 0) {
                playbackStateManager.registerPendingStart(queueItems.getOrNull(queueStartIndex)?.mediaId)
            } else {
                playbackStateManager.clearPendingStart()
            }
            scope.launch(Dispatchers.IO) {
                if (isLocalQueue) {
                    Logger.d(TAG, "Skipping remote queue replace for local items")
                    return@launch
                }
                playbackStateManager.reconnectLocalPlayerIfUnavailable()
                val queueId = playbackStateManager.currentQueueId ?: awaitQueueId()
                if (queueId.isNullOrBlank()) {
                    Logger.w(TAG, "No active queue available for play request")
                    return@launch
                }
                if (usePlaylistUri) {
                    val resolvedPlaylistUri = requireNotNull(playlistUri)
                    val resolvedStartItemUri = requireNotNull(startItemUri)
                    Logger.d(TAG, "Requesting playMediaItem queueId=$queueId option=REPLACE")
                    repository.playMediaItem(
                        queueId = queueId,
                        media = resolvedPlaylistUri,
                        option = QueueOption.REPLACE,
                        startItem = resolvedStartItemUri
                    ).onFailure { Logger.w(TAG, "Replace queue failed", it) }
                    return@launch
                }
                val uris = queueItems.mapNotNull { it.streamUri() }
                if (uris.isEmpty()) {
                    Logger.w(TAG, "No stream URIs resolved for play request")
                    return@launch
                }
                Logger.d(
                    TAG,
                    "Requesting playMedia queueId=$queueId option=REPLACE count=${uris.size}"
                )
                val playResult = repository.playMedia(queueId, uris, QueueOption.REPLACE)
                    .onFailure { Logger.w(TAG, "Replace queue failed", it) }
                if (queueStartIndex > 0 && playResult.isSuccess) {
                    repository.playIndex(queueId, queueStartIndex)
                        .onFailure { Logger.w(TAG, "Failed to set start index", it) }
                }
            }
            queueManager.replaceQueue(queueItems, queueStartIndex, startPositionMs)
            future.set(MediaSession.MediaItemsWithStartPosition(queueItems, queueStartIndex, startPositionMs))
        }
        return future
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

    private fun resolveMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
        if (mediaItems.isEmpty()) return mediaItems
        return mediaItems.map { item ->
            if (item.streamUri() != null) {
                item
            } else {
                mediaLibraryBrowser.resolveMediaItem(item.mediaId) ?: item
            }
        }
    }

    private fun MediaItem.streamUri(): String? {
        val streamingUri = mediaMetadata.extras?.getString(EXTRA_STREAM_URI)
        return if (!streamingUri.isNullOrBlank()) {
            streamingUri
        } else {
            localConfiguration?.uri?.toString()
        }
    }

    private suspend fun resolveRemainingQueue(
        resolvedItems: List<MediaItem>,
        startIndex: Int
    ): Pair<List<MediaItem>, Int> {
        if (resolvedItems.isEmpty()) return resolvedItems to startIndex
        val safeIndex = if (startIndex in resolvedItems.indices) startIndex else 0
        val startItem = resolvedItems[safeIndex]
        val parentMediaId = startItem.mediaMetadata.extras?.getString(EXTRA_PARENT_MEDIA_ID)
        if (parentMediaId.isNullOrBlank()) return resolvedItems to startIndex
        val resolvedMatchesParent = resolvedItems.size > 1 && resolvedItems.all { item ->
            item.mediaMetadata.extras?.getString(EXTRA_PARENT_MEDIA_ID) == parentMediaId
        }
        val parentItems = if (resolvedMatchesParent) {
            resolvedItems
        } else {
            withContext(Dispatchers.IO) {
                mediaLibraryBrowser.getParentTrackItems(parentMediaId)
            }
        }
        if (parentItems.isEmpty()) return resolvedItems to startIndex
        val matchIndex = if (parentItems === resolvedItems && startIndex in resolvedItems.indices) {
            startIndex
        } else {
            parentItems.indexOfFirst { it.mediaId == startItem.mediaId }
        }
        if (matchIndex == -1) return resolvedItems to startIndex
        return parentItems.subList(matchIndex, parentItems.size) to 0
    }

    private fun handlePlay() {
        Logger.i(TAG, "Play requested via MediaSession")
        playbackStateManager.notifyUserInitiatedPlayback()
        markPlaybackRequestedForMediaItem(player.currentMediaItem)
        player.play()
        if (isLocalQueueActive()) return
        scope.launch {
            playbackStateManager.reconnectLocalPlayerIfUnavailable()
            val queueId = playbackStateManager.currentQueueId ?: awaitQueueId()
            if (queueId.isNullOrBlank()) {
                Logger.w(TAG, "No active queue available for resume")
                return@launch
            }
            Logger.d(TAG, "Requesting resume queueId=$queueId")
            val resumeResult = repository.resumeQueue(queueId)
            if (resumeResult.isSuccess) return@launch
            val error = resumeResult.exceptionOrNull()
            Logger.w(TAG, "Resume command failed", error)
            if (!isNoPlayableItemError(error)) return@launch
            val fallbackIndex = resolvePlayableIndex(queueId)
            if (fallbackIndex == null) {
                Logger.w(TAG, "No playable queue item available for resume fallback")
                return@launch
            }
            Logger.d(TAG, "Resume failed; requesting playIndex=$fallbackIndex queueId=$queueId")
            repository.playIndex(queueId, fallbackIndex)
                .onFailure { Logger.w(TAG, "Resume fallback failed", it) }
        }
    }

    private fun handlePause() {
        playbackStateManager.notifyUserInitiatedPause()
        player.pause()
        if (isLocalQueueActive()) return
        val queueId = playbackStateManager.currentQueueId ?: return
        scope.launch {
            Logger.d(TAG, "Requesting pause queueId=$queueId")
            repository.pauseQueue(queueId)
                .onFailure { Logger.w(TAG, "Pause command failed", it) }
        }
    }

    private fun handleStop() {
        playbackStateManager.notifyUserInitiatedPause()
        player.stop()
        performanceMonitor.clearPlaybackRequests()
        if (isLocalQueueActive()) return
        val queueId = playbackStateManager.currentQueueId ?: return
        scope.launch {
            Logger.d(TAG, "Requesting stop (pause) queueId=$queueId")
            repository.pauseQueue(queueId)
                .onFailure { Logger.w(TAG, "Stop command failed", it) }
        }
    }

    private fun handleNext() {
        markPlaybackRequestedForIndex(player.nextMediaItemIndex)
        if (isLocalQueueActive()) return
        val queueId = playbackStateManager.currentQueueId ?: return
        scope.launch {
            Logger.d(TAG, "Requesting next queueId=$queueId")
            repository.nextTrack(queueId)
                .onFailure { Logger.w(TAG, "Next command failed", it) }
        }
    }

    private fun handlePrevious() {
        val shouldSkipToPrevious = shouldSkipToPreviousTrack()
        if (shouldSkipToPrevious) {
            markPlaybackRequestedForIndex(player.previousMediaItemIndex)
        }
        if (isLocalQueueActive()) return
        val queueId = playbackStateManager.currentQueueId ?: return
        if (!shouldSkipToPrevious) {
            playbackStateManager.suppressNextRemoteSeek()
        }
        scope.launch {
            if (shouldSkipToPrevious) {
                Logger.d(TAG, "Requesting previous queueId=$queueId")
                repository.previousTrack(queueId)
                    .onFailure { Logger.w(TAG, "Previous command failed", it) }
            } else {
                Logger.d(TAG, "Requesting seek to start queueId=$queueId")
                repository.seekTo(queueId, 0)
                    .onFailure { Logger.w(TAG, "Seek command failed", it) }
            }
        }
    }

    private fun shouldSkipToPreviousTrack(): Boolean {
        val hasPrevious = player.previousMediaItemIndex != C.INDEX_UNSET
        if (!hasPrevious) return false
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        return positionMs <= PREVIOUS_TRACK_THRESHOLD_MS
    }

    private fun handleShuffleModeChange(shuffleModeEnabled: Boolean) {
        if (isLocalQueueActive()) return
        if (shuffleModeEnabled == playbackStateManager.shuffle.value) return
        scope.launch(Dispatchers.IO) {
            val queueId = playbackStateManager.currentQueueId ?: awaitQueueId()
            if (queueId.isNullOrBlank()) {
                Logger.w(TAG, "No active queue available for shuffle toggle")
                return@launch
            }
            Logger.d(TAG, "Requesting shuffle=$shuffleModeEnabled queueId=$queueId")
            repository.setShuffleMode(queueId, shuffleModeEnabled)
                .onFailure { Logger.w(TAG, "Shuffle command failed", it) }
        }
    }

    private fun handleRepeatModeChange(@Player.RepeatMode repeatMode: Int) {
        if (isLocalQueueActive()) return
        val targetMode = repeatMode.toDomainRepeatMode()
        if (targetMode == playbackStateManager.repeatMode.value) return
        scope.launch(Dispatchers.IO) {
            val queueId = playbackStateManager.currentQueueId ?: awaitQueueId()
            if (queueId.isNullOrBlank()) {
                Logger.w(TAG, "No active queue available for repeat toggle")
                return@launch
            }
            Logger.d(TAG, "Requesting repeatMode=$targetMode queueId=$queueId")
            repository.setRepeatMode(queueId, targetMode)
                .onFailure { Logger.w(TAG, "Repeat mode command failed", it) }
        }
    }

    private fun @receiver:Player.RepeatMode Int.toDomainRepeatMode(): RepeatMode {
        return when (this) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }
    }

    private suspend fun awaitQueueId(): String? {
        return withTimeoutOrNull(QUEUE_ID_WAIT_TIMEOUT_MS) {
            playbackStateManager.queueIdFlow.first { !it.isNullOrBlank() }
        }
    }

    private suspend fun resolvePlayableIndex(queueId: String): Int? {
        val fallback = playbackStateManager.findPlayableIndexFromCurrent()
        val playerId = playbackStateManager.currentPlayerId ?: awaitPlayerId()
        if (playerId.isNullOrBlank()) return fallback
        val queue = repository.getActiveQueue(playerId, includeItems = true).getOrNull()
            ?: return fallback
        if (queue.queueId != queueId) return fallback
        return playbackStateManager.findPlayableIndex(queue) ?: fallback
    }

    private suspend fun awaitPlayerId(): String? {
        return withTimeoutOrNull(PLAYER_ID_WAIT_TIMEOUT_MS) {
            playbackStateManager.playerIdFlow.first { !it.isNullOrBlank() }
        }
    }

    private fun isNoPlayableItemError(error: Throwable?): Boolean {
        val message = error?.message ?: return false
        return message.contains("No playable item", ignoreCase = true)
    }

    private fun isLocalQueueActive(): Boolean {
        return queueManager.isLocalQueueActive()
    }

    companion object {
        private const val TAG = "PlaybackSessionCallback"
        private const val QUEUE_ID_WAIT_TIMEOUT_MS = 3000L
        private const val PLAYER_ID_WAIT_TIMEOUT_MS = 3000L
        private const val PREVIOUS_TRACK_THRESHOLD_MS = 3000L
    }
}
