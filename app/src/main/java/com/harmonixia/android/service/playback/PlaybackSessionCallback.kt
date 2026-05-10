package com.harmonixia.android.service.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcel
import android.util.LruCache
import androidx.core.graphics.scale
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
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowConversionToBitmap
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.size.Scale
import coil3.toBitmap
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.EXTRA_PARENT_MEDIA_ID
import com.harmonixia.android.util.EXTRA_STREAM_URI
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.PerformanceMonitor
import com.harmonixia.android.util.resolvePlaybackStreamUri
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@UnstableApi
class PlaybackSessionCallback(
    private val player: Player,
    private val repository: MusicAssistantRepository,
    private val playbackStateManager: PlaybackStateManager,
    private val mediaLibraryBrowser: MediaLibraryBrowser,
    private val performanceMonitor: PerformanceMonitor,
    private val scope: CoroutineScope,
    private val context: Context,
    private val imageLoader: ImageLoader
) : MediaLibrarySession.Callback {

    private val autoArtworkCache = object : LruCache<String, ByteArray>(AUTO_ARTWORK_CACHE_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }
    private val autoArtworkSemaphore = Semaphore(AUTO_ARTWORK_PARALLELISM)
    private val autoArtworkFailureLogBudget = AtomicInteger(AUTO_ARTWORK_LOG_FAILURES)

    private val playerListener = object : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason != Player.DISCONTINUITY_REASON_SEEK) return
            if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex) return
            if (playbackStateManager.consumeSyncSeekSuppression()) return
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

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onPlayerCommandRequest(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        @Player.Command playerCommand: Int
    ): Int {
        return when (playerCommand) {
            Player.COMMAND_PLAY_PAUSE -> {
                if (player.isPlaying) handlePause() else handlePlay()
                SessionResult.RESULT_SUCCESS
            }
            Player.COMMAND_STOP -> {
                handleStop()
                SessionResult.RESULT_SUCCESS
            }
            Player.COMMAND_SEEK_TO_NEXT -> {
                handleNext()
                SessionResult.RESULT_SUCCESS
            }
            Player.COMMAND_SEEK_TO_PREVIOUS -> {
                handlePrevious()
                SessionResult.RESULT_SUCCESS
            }
            else -> SessionResult.RESULT_ERROR_NOT_SUPPORTED
        }
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
                        .setTitle(context.getString(R.string.app_name))
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
            val startTimestamp = System.currentTimeMillis()
            val useBuckets = shouldUseAutoBrowseBuckets(browser)
            val children = runCatching {
                mediaLibraryBrowser.getChildren(parentId, page, pageSize, useBuckets)
            }.getOrElse {
                Logger.w(TAG, "Failed to load children for $parentId", it)
                emptyList()
            }
            val embedArtwork = shouldEmbedArtwork(browser)
            val resolvedChildren = if (embedArtwork) {
                attachArtworkData(children)
            } else {
                children
            }
            if (resolvedChildren.size > LARGE_BROWSE_COUNT) {
                runCatching {
                    val parcel = Parcel.obtain()
                    val bundles = ArrayList<android.os.Bundle>(resolvedChildren.size)
                    for (item in resolvedChildren) {
                        bundles.add(toBundleForParcelMeasurement(item))
                    }
                    parcel.writeTypedList(bundles)
                    val size = parcel.dataSize()
                    parcel.recycle()
                    Logger.d(
                        TAG,
                        "Auto browse children parentId=$parentId bundleBytes=$size items=${resolvedChildren.size}"
                    )
                }.onFailure {
                    Logger.w(TAG, "Failed to measure browse parcel size for $parentId", it)
                }
            }
            Logger.d(
                TAG,
                "Auto browse children parentId=$parentId page=$page pageSize=$pageSize count=${resolvedChildren.size} controller=${browser.packageName} embedArtwork=$embedArtwork buckets=$useBuckets durationMs=${System.currentTimeMillis() - startTimestamp}"
            )
            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(resolvedChildren), params))
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
            val resolvedItems = if (shouldEmbedArtwork(browser)) {
                attachArtworkData(items)
            } else {
                items
            }
            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(resolvedItems), params))
        }
        return future
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        val resolvedItems = resolveMediaItems(mediaItems)
        playbackStateManager.notifyUserInitiatedPlayback()
        markPlaybackRequestedForMediaItem(resolvedItems.firstOrNull())
        scope.launch(Dispatchers.IO) {
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
            playbackStateManager.refreshQueueFast()
        }
        return Futures.immediateFuture(emptyList())
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
            playbackStateManager.notifyUserInitiatedPlayback()
            markPlaybackRequestedForStartItem(queueItems, queueStartIndex)
            if (queueStartIndex > 0) {
                playbackStateManager.registerPendingStart(queueItems.getOrNull(queueStartIndex)?.mediaId)
            } else {
                playbackStateManager.clearPendingStart()
            }
            scope.launch(Dispatchers.IO) {
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
                    playbackStateManager.refreshQueueFast()
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
                playbackStateManager.refreshQueueFast()
            }
            val currentItems = currentPlayerItems()
            val returnIndex = player.currentMediaItemIndex
                .takeIf { it in currentItems.indices }
                ?: 0
            val returnPosition = if (currentItems.isEmpty()) 0L else player.currentPosition
            future.set(
                MediaSession.MediaItemsWithStartPosition(
                    currentItems,
                    returnIndex,
                    returnPosition
                )
            )
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
        return resolvePlaybackStreamUri(
            extrasStreamUri = mediaMetadata.extras?.getString(EXTRA_STREAM_URI),
            localConfigurationUri = localConfiguration?.uri?.toString()
        )
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
        val queueId = playbackStateManager.currentQueueId ?: return
        scope.launch {
            Logger.d(TAG, "Requesting stop (pause) queueId=$queueId")
            repository.pauseQueue(queueId)
                .onFailure { Logger.w(TAG, "Stop command failed", it) }
        }
    }

    private fun handleNext() {
        markPlaybackRequestedForIndex(player.nextMediaItemIndex)
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

    private fun currentPlayerItems(): List<MediaItem> {
        val count = player.mediaItemCount
        if (count <= 0) return emptyList()
        return (0 until count).map { index -> player.getMediaItemAt(index) }
    }

    private fun shouldEmbedArtwork(controller: MediaSession.ControllerInfo): Boolean {
        val packageName = controller.packageName.orEmpty()
        if (packageName.isBlank()) return false
        if (packageName in AUTO_CONTROLLER_PACKAGES) return true
        if (packageName.startsWith("com.google.android.apps.auto.")) return true
        if (packageName.startsWith("com.android.car.")) return true
        return false
    }

    private fun shouldUseAutoBrowseBuckets(controller: MediaSession.ControllerInfo): Boolean {
        val packageName = controller.packageName.orEmpty()
        if (packageName.isBlank()) return false
        if (packageName in AUTO_CONTROLLER_PACKAGES) return true
        if (packageName.startsWith("com.google.android.apps.auto.")) return true
        if (packageName.startsWith("com.android.car.")) return true
        return false
    }

    private suspend fun attachArtworkData(items: List<MediaItem>): List<MediaItem> = coroutineScope {
        if (items.isEmpty()) return@coroutineScope items
        val limit = AUTO_ARTWORK_MAX_ITEMS
        val (candidates, remainder) = if (items.size > limit) {
            items.take(limit) to items.drop(limit)
        } else {
            items to emptyList()
        }
        val processed = candidates.map { item ->
            async(Dispatchers.IO) {
                autoArtworkSemaphore.withPermit {
                    ensureArtworkData(item)
                }
            }
        }.awaitAll()
        if (remainder.isEmpty()) processed else processed + remainder
    }

    private suspend fun ensureArtworkData(item: MediaItem): MediaItem {
        val metadata = item.mediaMetadata
        if (metadata.artworkData != null) return item
        val artworkUri = metadata.artworkUri ?: return item
        val cacheKey = artworkUri.toString()
        val cached = synchronized(autoArtworkCache) { autoArtworkCache.get(cacheKey) }
        val artworkData = cached ?: loadArtworkBytes(artworkUri)?.also { bytes ->
            synchronized(autoArtworkCache) { autoArtworkCache.put(cacheKey, bytes) }
        } ?: return item
        val updatedMetadata = metadata.buildUpon()
            .setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .build()
        return item.buildUpon()
            .setMediaMetadata(updatedMetadata)
            .build()
    }

    private suspend fun loadArtworkBytes(uri: Uri): ByteArray? {
        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(AUTO_ARTWORK_SIZE_PX)
                .scale(Scale.FIT)
                .allowHardware(false)
                .allowConversionToBitmap(true)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build()
            val result = imageLoader.execute(request)
            val success = result as? SuccessResult ?: run {
                if (autoArtworkFailureLogBudget.getAndDecrement() > 0) {
                    Logger.w(TAG, "Auto artwork load failed uri=$uri result=$result")
                }
                return@runCatching null
            }
            val bitmap = success.image.toBitmap()
            val scaled = scaleDown(bitmap, AUTO_ARTWORK_SIZE_PX)
            val encoded = encodeBitmap(scaled)
            if (scaled != bitmap) {
                scaled.recycle()
            }
            encoded
        }.getOrNull()
    }

    private fun scaleDown(bitmap: Bitmap, maxSize: Int): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim <= maxSize) return bitmap
        val scale = maxSize / maxDim.toFloat()
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return bitmap.scale(width, height, filter = true)
    }

    @Suppress("DEPRECATION")
    private fun toBundleForParcelMeasurement(item: MediaItem): android.os.Bundle {
        // Media3 currently only exposes deprecated bundle serialization for MediaItem.
        return item.toBundle()
    }

    private fun encodeBitmap(bitmap: Bitmap): ByteArray? {
        val output = ByteArrayOutputStream()
        var quality = AUTO_ARTWORK_JPEG_QUALITY
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) return null
        var bytes = output.toByteArray()
        while (bytes.size > AUTO_ARTWORK_MAX_BYTES && quality > AUTO_ARTWORK_MIN_QUALITY) {
            quality -= AUTO_ARTWORK_QUALITY_STEP
            output.reset()
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) break
            bytes = output.toByteArray()
        }
        return bytes.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val TAG = "PlaybackSessionCallback"
        private const val LARGE_BROWSE_COUNT = 500
        private const val QUEUE_ID_WAIT_TIMEOUT_MS = 3000L
        private const val PLAYER_ID_WAIT_TIMEOUT_MS = 3000L
        private const val PREVIOUS_TRACK_THRESHOLD_MS = 3000L
        private const val AUTO_ARTWORK_SIZE_PX = 256
        private const val AUTO_ARTWORK_MAX_BYTES = 64 * 1024
        private const val AUTO_ARTWORK_JPEG_QUALITY = 75
        private const val AUTO_ARTWORK_MIN_QUALITY = 45
        private const val AUTO_ARTWORK_QUALITY_STEP = 10
        private const val AUTO_ARTWORK_CACHE_BYTES = 8 * 1024 * 1024
        private const val AUTO_ARTWORK_PARALLELISM = 3
        private const val AUTO_ARTWORK_LOG_FAILURES = 8
        private const val AUTO_ARTWORK_MAX_ITEMS = 40
        private val AUTO_CONTROLLER_PACKAGES = setOf(
            "com.google.android.projection.gearhead",
            "com.google.android.apps.auto.media",
            "com.google.android.apps.auto.media.player",
            "com.android.car.media",
            "com.android.car.carlauncher"
        )
    }
}
