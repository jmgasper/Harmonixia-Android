package com.harmonixia.android.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.bitmapConfig
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.ImageQualityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink

class PlaylistCoverGenerator(
    private val context: Context,
    private val repository: MusicAssistantRepository,
    private val imageLoader: ImageLoader
) {
    private val qualityManager = ImageQualityManager(context)

    suspend fun getCoverPath(playlist: Playlist, sizePx: Int): String? = withContext(Dispatchers.IO) {
        val diskCache = imageLoader.diskCache ?: return@withContext null
        val tracksResult = repository.getPlaylistTracks(playlist.itemId, playlist.provider)
        val tracks = tracksResult.getOrDefault(emptyList())
        val imageUrls = tracks.mapNotNull { it.imageUrl }.distinct().take(MAX_IMAGES)
        if (imageUrls.isEmpty()) return@withContext null

        val signature = tracks.take(MAX_IMAGES).joinToString("|") { it.itemId }
        val cacheKey = "playlist_cover_${playlist.provider}_${playlist.itemId}_${sizePx}_${signature.hashCode()}"

        diskCache.openSnapshot(cacheKey)?.use { snapshot ->
            return@withContext snapshot.data.toString()
        }

        val tileSize = (sizePx / 2f).toInt().coerceAtLeast(1)
        val bitmaps = imageUrls.mapNotNull { url ->
            loadBitmap(url, tileSize)
        }
        if (bitmaps.isEmpty()) return@withContext null

        val composed = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composed)
        canvas.drawColor(Color.DKGRAY)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, tileSize, tileSize)
        bitmaps.take(MAX_IMAGES).forEachIndexed { index, bitmap ->
            val left = (index % 2) * tileSize
            val top = (index / 2) * tileSize
            rect.offsetTo(left, top)
            canvas.drawBitmap(bitmap, null, rect, paint)
        }

        writeToDiskCache(diskCache, cacheKey, signature, composed)

        diskCache.openSnapshot(cacheKey)?.use { snapshot ->
            return@withContext snapshot.data.toString()
        }
        return@withContext null
    }

    private suspend fun loadBitmap(url: String, sizePx: Int): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(sizePx)
            .bitmapConfig(qualityManager.getOptimalBitmapConfig())
            .build()
        val result = imageLoader.execute(request)
        val image = (result as? SuccessResult)?.image ?: return null
        return if (image is BitmapImage) {
            image.bitmap
        } else {
            val bitmap = Bitmap.createBitmap(
                image.width.coerceAtLeast(1),
                image.height.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            image.draw(canvas)
            bitmap
        }
    }

    private fun writeToDiskCache(
        diskCache: DiskCache,
        cacheKey: String,
        signature: String,
        bitmap: Bitmap
    ) {
        val editor = diskCache.openEditor(cacheKey) ?: return
        try {
            val fileSystem = diskCache.fileSystem
            fileSystem.sink(editor.metadata).buffer().use { sink ->
                sink.writeUtf8(signature)
            }
            fileSystem.sink(editor.data).buffer().use { sink ->
                sink.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, output)
                }
            }
            editor.commit()
        } catch (_: Exception) {
            editor.abort()
        }
    }

    private companion object {
        private const val MAX_IMAGES = 4
    }
}
