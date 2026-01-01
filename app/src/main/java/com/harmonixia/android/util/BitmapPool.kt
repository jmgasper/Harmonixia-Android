package com.harmonixia.android.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import coil3.Extras

class BitmapPool private constructor(
    maxSizeBytes: Long
) {
    private val cache = object : LruCache<Key, Bitmap>(maxSizeBytes.toCacheSize()) {
        override fun sizeOf(key: Key, value: Bitmap): Int {
            return value.allocationByteCount
        }
    }

    @Synchronized
    fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        val bitmap = cache.remove(Key(width, height, config))
        return if (bitmap?.isRecycled == false && bitmap.isMutable) {
            bitmap
        } else {
            null
        }
    }

    @Synchronized
    fun put(bitmap: Bitmap) {
        if (bitmap.isRecycled || !bitmap.isMutable) {
            return
        }
        val config = bitmap.config ?: return
        cache.put(Key(bitmap.width, bitmap.height, config), bitmap)
    }

    class Builder {
        private var maxSizeBytes: Long? = null

        fun maxSizeBytes(size: Long) = apply {
            require(size > 0L) { "maxSizeBytes must be greater than 0." }
            maxSizeBytes = size
        }

        fun maxSizePercent(context: Context, percent: Double) = apply {
            require(percent in 0.0..1.0) { "percent must be in the range [0.0, 1.0]." }
            val activityManager = checkNotNull(context.getSystemService(ActivityManager::class.java)) {
                "ActivityManager unavailable."
            }
            val memoryClassBytes = activityManager.memoryClass * 1024L * 1024L
            maxSizeBytes = (memoryClassBytes * percent).toLong().coerceAtLeast(1L)
        }

        fun build(): BitmapPool {
            val size = checkNotNull(maxSizeBytes) { "maxSizeBytes must be set." }
            return BitmapPool(size)
        }
    }

    private data class Key(
        val width: Int,
        val height: Int,
        val config: Bitmap.Config
    )

    private fun Long.toCacheSize(): Int {
        return coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    companion object {
        val EXTRAS_KEY = Extras.Key<BitmapPool?>(default = null)
    }
}
