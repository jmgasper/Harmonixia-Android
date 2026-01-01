package com.harmonixia.android.service.playback

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.harmonixia.android.domain.model.Queue
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import com.harmonixia.android.util.EXTRA_IS_LOCAL_FILE
import com.harmonixia.android.util.EXTRA_STREAM_URI
import com.harmonixia.android.util.EXTRA_TRACK_QUALITY
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.matchesLocal
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class QueueManager(
    private val repository: MusicAssistantRepository,
    private val localMediaRepository: LocalMediaRepository
) {
    private val queueItems = mutableListOf<MediaItem>()
    private var currentIndex: Int = 0
    private var queueId: String? = null
    private var player: ExoPlayer? = null
    private val playedInShuffleSession = mutableSetOf<String>()
    private var isShuffleActive = false
    private var lastMediaItemId: String? = null
    private var lastLocalResolutionMediaId: String? = null
    private var retainQueueUntilMs: Long = 0L
    private var optimisticCurrentIndex: Int? = null
    private var optimisticIndexSetAtMs: Long = 0L

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = player ?: return
            val previousMediaId = lastMediaItemId
            currentIndex = player.currentMediaItemIndex
            if (isShuffleActive && !previousMediaId.isNullOrBlank()) {
                playedInShuffleSession.add(previousMediaId)
                if (queueItems.isNotEmpty() && playedInShuffleSession.size >= queueItems.size) {
                    playedInShuffleSession.clear()
                }
            }
            val skipped = if (isShuffleActive && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                maybeSkipPlayedShuffleItem(player, mediaItem)
            } else {
                false
            }
            lastMediaItemId = if (skipped) previousMediaId else mediaItem?.mediaId
        }
    }

    fun attachPlayer(player: ExoPlayer) {
        this.player = player
        player.addListener(playerListener)
    }

    fun updateQueueId(queueId: String?) {
        if (this.queueId != queueId) {
            resetLocalResolutionState()
        }
        this.queueId = queueId
    }

    fun updateShuffleState(shuffle: Boolean) {
        if (!shuffle && isShuffleActive) {
            playedInShuffleSession.clear()
        }
        isShuffleActive = shuffle
    }

    suspend fun buildMediaItem(track: Track, resolveLocal: Boolean = false): MediaItem {
        val localFile = when {
            resolveLocal -> resolveLocalFile(track)
            track.provider == OFFLINE_PROVIDER &&
                track.uri.isNotBlank() &&
                Uri.parse(track.uri).scheme.isNullOrBlank() -> File(track.uri)
            else -> null
        }
        if (resolveLocal && track.itemId.isNotBlank()) {
            lastLocalResolutionMediaId = track.itemId
        }
        return buildMediaItemInternal(track, localFile)
    }

    private fun buildMediaItemInternal(track: Track, localFile: File?): MediaItem {
        val isLocalFile = localFile != null
        val durationMs = track.lengthSeconds
            .takeIf { it > 0 }
            ?.toLong()
            ?.times(1000L)
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.imageUrl?.let { Uri.parse(it) })
            .setDurationMs(durationMs)
        val extras = Bundle().apply {
            if (!track.quality.isNullOrBlank()) {
                putString(EXTRA_TRACK_QUALITY, track.quality)
            }
            putBoolean(EXTRA_IS_LOCAL_FILE, isLocalFile)
            putString(EXTRA_STREAM_URI, track.uri)
        }
        metadataBuilder.setExtras(extras)
        return MediaItem.Builder()
            .setUri(localFile?.let { Uri.fromFile(it) } ?: Uri.parse(track.uri))
            .setMediaId(track.itemId)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    suspend fun updateFromRemote(queue: Queue) {
        val items = mutableListOf<MediaItem>()
        for (track in queue.items) {
            items.add(buildMediaItem(track, resolveLocal = false))
        }
        if (items.isEmpty()) {
            val currentItem = queue.currentItem
            if (currentItem != null) {
                resetLocalResolutionState()
                clearOptimisticIndex()
                val mediaItem = buildMediaItem(currentItem, resolveLocal = false)
                queueItems.clear()
                queueItems.add(mediaItem)
                currentIndex = 0
                player?.setMediaItems(queueItems, currentIndex, C.TIME_UNSET)
                player?.prepare()
                clearRetention()
                return
            }
            if (shouldRetainQueueOnEmpty()) {
                return
            }
            resetLocalResolutionState()
            clearOptimisticIndex()
            queueItems.clear()
            currentIndex = 0
            player?.clearMediaItems()
            clearRetention()
            return
        }
        clearRetention()
        val changed = items.size != queueItems.size ||
            items.map { it.mediaId } != queueItems.map { it.mediaId }
        val remoteIndex = queue.currentIndex.coerceAtLeast(0).coerceAtMost(items.lastIndex)
        val optimisticIndex = optimisticCurrentIndex
            ?.coerceAtLeast(0)
            ?.coerceAtMost(items.lastIndex)
        val withinOptimisticWindow = optimisticIndex != null &&
            SystemClock.elapsedRealtime() - optimisticIndexSetAtMs < OPTIMISTIC_INDEX_RETENTION_MS
        val useOptimisticIndex = withinOptimisticWindow && !changed
        if (!withinOptimisticWindow || changed) {
            clearOptimisticIndex()
        }
        val resolvedIndex = if (useOptimisticIndex) optimisticIndex ?: remoteIndex else remoteIndex
        currentIndex = resolvedIndex
        if (changed) {
            resetLocalResolutionState()
            queueItems.clear()
            queueItems.addAll(items)
            player?.setMediaItems(queueItems, currentIndex, C.TIME_UNSET)
            player?.prepare()
        }
    }

    private fun shouldRetainQueueOnEmpty(): Boolean {
        if (queueItems.isEmpty()) return false
        return SystemClock.elapsedRealtime() < retainQueueUntilMs
    }

    private fun clearRetention() {
        retainQueueUntilMs = 0L
    }

    fun replaceQueue(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        resetLocalResolutionState()
        queueItems.clear()
        queueItems.addAll(mediaItems)
        currentIndex = startIndex.coerceAtLeast(0)
        optimisticCurrentIndex = currentIndex
        optimisticIndexSetAtMs = SystemClock.elapsedRealtime()
        player?.setMediaItems(queueItems, currentIndex, startPositionMs)
        player?.prepare()
        val until = SystemClock.elapsedRealtime() + DEFAULT_QUEUE_SEED_RETENTION_MS
        if (until > retainQueueUntilMs) {
            retainQueueUntilMs = until
        }
    }

    fun addMediaItems(mediaItems: List<MediaItem>) {
        queueItems.addAll(mediaItems)
        player?.addMediaItems(mediaItems)
    }

    fun playNextMediaItems(mediaItems: List<MediaItem>) {
        val insertIndex = (player?.currentMediaItemIndex ?: currentIndex) + 1
        val safeIndex = insertIndex.coerceAtMost(queueItems.size)
        queueItems.addAll(safeIndex, mediaItems)
        player?.addMediaItems(safeIndex, mediaItems)
    }

    suspend fun playAlbum(tracks: List<Track>, startIndex: Int = 0): Result<Unit> {
        val queueId = queueId ?: return Result.failure(IllegalStateException("Queue ID unavailable"))
        val uris = tracks.map { it.uri }
        resetLocalResolutionState()
        val mediaItems = buildMediaItems(tracks)
        queueItems.clear()
        queueItems.addAll(mediaItems)
        currentIndex = startIndex.coerceAtLeast(0).coerceAtMost((mediaItems.lastIndex).coerceAtLeast(0))
        player?.setMediaItems(queueItems, currentIndex, C.TIME_UNSET)
        player?.prepare()
        val result = repository.playMedia(queueId, uris, QueueOption.REPLACE)
        if (startIndex > 0) {
            repository.playIndex(queueId, currentIndex)
                .onFailure { Logger.w(TAG, "Failed to set start index", it) }
        }
        return result
    }

    suspend fun addToQueue(tracks: List<Track>): Result<Unit> {
        val queueId = queueId ?: return Result.failure(IllegalStateException("Queue ID unavailable"))
        val uris = tracks.map { it.uri }
        val mediaItems = buildMediaItems(tracks)
        queueItems.addAll(mediaItems)
        player?.addMediaItems(mediaItems)
        return repository.playMedia(queueId, uris, QueueOption.ADD)
    }

    suspend fun playNext(tracks: List<Track>): Result<Unit> {
        val queueId = queueId ?: return Result.failure(IllegalStateException("Queue ID unavailable"))
        val uris = tracks.map { it.uri }
        val mediaItems = buildMediaItems(tracks)
        playNextMediaItems(mediaItems)
        return repository.playMedia(queueId, uris, QueueOption.NEXT)
    }

    suspend fun clearQueue(): Result<Unit> {
        val queueId = queueId ?: return Result.failure(IllegalStateException("Queue ID unavailable"))
        resetLocalResolutionState()
        clearOptimisticIndex()
        queueItems.clear()
        player?.clearMediaItems()
        return repository.clearQueue(queueId)
    }

    fun currentQueue(): List<MediaItem> = queueItems.toList()

    fun currentIndex(): Int = currentIndex

    private suspend fun buildMediaItems(tracks: List<Track>): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        for (track in tracks) {
            items.add(buildMediaItem(track, resolveLocal = false))
        }
        return items
    }

    suspend fun ensureLocalForCurrentTrack(track: Track) {
        val player = player ?: return
        val mediaId = player.currentMediaItem?.mediaId ?: return
        if (mediaId != track.itemId) return
        if (mediaId == lastLocalResolutionMediaId) return

        val currentPosition = player.currentPosition
        val localFile = resolveLocalFile(track)
        lastLocalResolutionMediaId = mediaId
        if (localFile == null) return

        val currentMediaId = player.currentMediaItem?.mediaId
        if (currentMediaId != track.itemId) return
        val index = player.currentMediaItemIndex
        if (index !in queueItems.indices) return
        val mediaItem = buildMediaItemInternal(track, localFile)
        queueItems[index] = mediaItem
        player.replaceMediaItem(index, mediaItem)
        player.seekTo(index, currentPosition)
    }

    private suspend fun resolveLocalFile(track: Track): File? {
        val localPath = if (track.provider == OFFLINE_PROVIDER) {
            track.uri
        } else {
            resolveMappedLocalPath(track) ?: resolveMatchedLocalPath(track)
        }
        if (localPath.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            val file = File(localPath)
            if (file.exists() && file.length() > 0L) {
                file
            } else {
                Logger.w(TAG, "Local file missing or empty for ${track.itemId}; falling back to stream")
                null
            }
        }
    }

    private suspend fun resolveMappedLocalPath(track: Track): String? {
        val mapping = track.providerMappings.firstOrNull { provider ->
            provider.providerDomain == OFFLINE_PROVIDER || provider.providerInstance == OFFLINE_PROVIDER
        } ?: return null
        val mappedPath = Uri.decode(mapping.itemId).trim()
        if (mappedPath.isBlank()) return null
        return localMediaRepository.getTrackByFilePath(mappedPath).first()?.uri
    }

    private suspend fun resolveMatchedLocalPath(track: Track): String? {
        val title = track.title.trim()
        if (title.isBlank()) return null
        val candidates = localMediaRepository.searchTracks(title).first()
        if (candidates.isEmpty()) return null
        val normalizedTitle = normalizeMatchKey(track.title)
        val trackNumber = track.trackNumber.takeIf { it > 0 }
        val titleMatches = candidates.filter { normalizeMatchKey(it.title) == normalizedTitle }
        if (titleMatches.isEmpty()) return null
        val metadataMatches = titleMatches.filter { candidate ->
            track.matchesLocal(candidate)
        }.ifEmpty { titleMatches }
        val match = trackNumber?.let { number ->
            metadataMatches.firstOrNull { it.trackNumber == number }
        } ?: metadataMatches.firstOrNull()
        return match?.uri
    }

    private fun normalizeMatchKey(value: String): String {
        return value.trim().lowercase()
    }

    private fun maybeSkipPlayedShuffleItem(player: ExoPlayer, mediaItem: MediaItem?): Boolean {
        val mediaId = mediaItem?.mediaId ?: return false
        if (queueItems.size <= 1) return false
        if (!playedInShuffleSession.contains(mediaId)) return false
        val targetIndex = findUnplayedShuffleIndex(mediaId) ?: return false
        currentIndex = targetIndex
        player.seekTo(targetIndex, C.TIME_UNSET)
        return true
    }

    private fun findUnplayedShuffleIndex(excludeMediaId: String): Int? {
        val candidates = queueItems.withIndex()
            .filter { (index, item) ->
                index != currentIndex &&
                    item.mediaId != excludeMediaId &&
                    !playedInShuffleSession.contains(item.mediaId)
            }
        if (candidates.isEmpty()) return null
        return candidates.random().index
    }

    private fun resetLocalResolutionState() {
        lastLocalResolutionMediaId = null
    }

    private fun clearOptimisticIndex() {
        optimisticCurrentIndex = null
        optimisticIndexSetAtMs = 0L
    }

    companion object {
        private const val TAG = "QueueManager"
        private const val DEFAULT_QUEUE_SEED_RETENTION_MS = 60_000L
        private const val OPTIMISTIC_INDEX_RETENTION_MS = 3_000L
    }
}
