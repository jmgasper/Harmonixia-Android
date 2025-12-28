package com.harmonixia.android.service.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.harmonixia.android.domain.model.Queue
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger

class QueueManager(
    private val repository: MusicAssistantRepository
) {
    private val queueItems = mutableListOf<MediaItem>()
    private var currentIndex: Int = 0
    private var queueId: String? = null
    private var player: ExoPlayer? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = player ?: return
            currentIndex = player.currentMediaItemIndex
        }
    }

    fun attachPlayer(player: ExoPlayer) {
        this.player = player
        player.addListener(playerListener)
    }

    fun updateQueueId(queueId: String?) {
        this.queueId = queueId
    }

    fun buildMediaItem(track: Track): MediaItem {
        val durationMs = track.lengthSeconds
            .takeIf { it > 0 }
            ?.toLong()
            ?.times(1000L)
        return MediaItem.Builder()
            .setUri(track.uri)
            .setMediaId(track.itemId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.imageUrl?.let { Uri.parse(it) })
                    .setDurationMs(durationMs)
                    .build()
            )
            .build()
    }

    fun updateFromRemote(queue: Queue) {
        val items = queue.items.map { buildMediaItem(it) }
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
        val mediaItems = tracks.map { buildMediaItem(it) }
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
        val mediaItems = tracks.map { buildMediaItem(it) }
        queueItems.addAll(mediaItems)
        player?.addMediaItems(mediaItems)
        return repository.playMedia(queueId, uris, QueueOption.ADD)
    }

    suspend fun playNext(tracks: List<Track>): Result<Unit> {
        val queueId = queueId ?: return Result.failure(IllegalStateException("Queue ID unavailable"))
        val uris = tracks.map { it.uri }
        val mediaItems = tracks.map { buildMediaItem(it) }
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

    companion object {
        private const val TAG = "QueueManager"
    }
}
