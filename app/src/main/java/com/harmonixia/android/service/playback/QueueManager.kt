package com.harmonixia.android.service.playback

import android.net.Uri
import android.os.Bundle
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
        this.queueId = queueId
    }

    fun updateShuffleState(shuffle: Boolean) {
        if (!shuffle && isShuffleActive) {
            playedInShuffleSession.clear()
        }
        isShuffleActive = shuffle
    }

    suspend fun buildMediaItem(track: Track): MediaItem {
        val localFile = resolveLocalFile(track)
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
            items.add(buildMediaItem(track))
        }
        if (items.isEmpty()) {
            queueItems.clear()
            currentIndex = 0
            player?.clearMediaItems()
            return
        }
        val changed = items.size != queueItems.size ||
            items.map { it.mediaId } != queueItems.map { it.mediaId }
        currentIndex = queue.currentIndex.coerceAtLeast(0).coerceAtMost(items.lastIndex)
        if (changed) {
            queueItems.clear()
            queueItems.addAll(items)
            player?.setMediaItems(queueItems, currentIndex, C.TIME_UNSET)
            player?.prepare()
        }
    }

    fun replaceQueue(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        queueItems.clear()
        queueItems.addAll(mediaItems)
        currentIndex = startIndex.coerceAtLeast(0)
        player?.setMediaItems(queueItems, currentIndex, startPositionMs)
        player?.prepare()
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
        queueItems.clear()
        player?.clearMediaItems()
        return repository.clearQueue(queueId)
    }

    fun currentQueue(): List<MediaItem> = queueItems.toList()

    fun currentIndex(): Int = currentIndex

    private suspend fun buildMediaItems(tracks: List<Track>): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        for (track in tracks) {
            items.add(buildMediaItem(track))
        }
        return items
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

    companion object {
        private const val TAG = "QueueManager"
    }
}
