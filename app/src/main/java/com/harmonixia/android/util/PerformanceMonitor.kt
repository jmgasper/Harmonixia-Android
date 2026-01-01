package com.harmonixia.android.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitor @Inject constructor() {
    private val requestTimes = LinkedHashMap<String, Long>()
    private val latencyWindow = ArrayDeque<Long>(WINDOW_SIZE)
    private val detailStartTimes = LinkedHashMap<String, Long>()
    private val cachedDetailLatencies = detailTypeMap { ArrayDeque<Long>(DETAIL_LATENCY_WINDOW) }
    private val freshDetailLatencies = detailTypeMap { ArrayDeque<Long>(DETAIL_LATENCY_WINDOW) }
    private val trackLoadSamples = detailTypeMap { ArrayDeque<TrackLoadSample>(TRACK_LOAD_WINDOW) }
    private var playlistCacheHits = 0
    private var playlistCacheMisses = 0
    private var albumCacheHits = 0
    private var albumCacheMisses = 0

    fun markPlaybackRequested(trackId: String) {
        if (trackId.isBlank()) return
        val now = System.currentTimeMillis()
        synchronized(lock) {
            requestTimes.remove(trackId)
            requestTimes[trackId] = now
            pruneStaleRequestsLocked(now)
            trimRequestMapToMaxLocked()
        }
    }

    fun markPlaybackStarted(trackId: String) {
        if (trackId.isBlank()) return
        val startTime = System.currentTimeMillis()
        val requestTime = synchronized(lock) {
            pruneStaleRequestsLocked(startTime)
            requestTimes.remove(trackId)
        }
        if (requestTime != null) {
            val latency = (startTime - requestTime).coerceAtLeast(0)
            synchronized(lock) {
                latencyWindow.addLast(latency)
                while (latencyWindow.size > WINDOW_SIZE) {
                    latencyWindow.removeFirst()
                }
            }
            Logger.d(TAG, "Playback start latency for $trackId: ${latency}ms")
        }
    }

    fun clearPlaybackRequests() {
        synchronized(lock) {
            requestTimes.clear()
        }
    }

    fun getAveragePlaybackLatency(): Long {
        synchronized(lock) {
            if (latencyWindow.isEmpty()) return 0L
            val sum = latencyWindow.sum()
            return sum / latencyWindow.size
        }
    }

    fun markDetailLoadStart(type: DetailType, key: String) {
        if (key.isBlank()) return
        val now = System.currentTimeMillis()
        synchronized(lock) {
            detailStartTimes[detailKey(type, key)] = now
            pruneDetailStartsLocked(now)
        }
    }

    fun markDetailCacheShown(type: DetailType, key: String) {
        recordDetailLatency(type, key, cachedDetailLatencies[type] ?: return)
    }

    fun markDetailFreshLoaded(type: DetailType, key: String) {
        recordDetailLatency(type, key, freshDetailLatencies[type] ?: return)
    }

    fun markTrackListLoaded(type: DetailType, key: String, trackCount: Int) {
        val safeCount = trackCount.coerceAtLeast(0)
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val startedAt = detailStartTimes.remove(detailKey(type, key)) ?: return
            val duration = (now - startedAt).coerceAtLeast(0)
            val samples = trackLoadSamples[type] ?: return
            samples.addLast(TrackLoadSample(safeCount, duration))
            while (samples.size > TRACK_LOAD_WINDOW) {
                samples.removeFirst()
            }
        }
    }

    fun recordCacheLookup(type: CacheType, hit: Boolean) {
        synchronized(lock) {
            when (type) {
                CacheType.PLAYLIST -> if (hit) playlistCacheHits += 1 else playlistCacheMisses += 1
                CacheType.ALBUM -> if (hit) albumCacheHits += 1 else albumCacheMisses += 1
            }
        }
    }

    fun getCacheHitRate(type: CacheType): Int? {
        synchronized(lock) {
            val (hits, misses) = when (type) {
                CacheType.PLAYLIST -> playlistCacheHits to playlistCacheMisses
                CacheType.ALBUM -> albumCacheHits to albumCacheMisses
            }
            val total = hits + misses
            if (total == 0) return null
            return ((hits.toDouble() / total.toDouble()) * 100).toInt()
        }
    }

    fun getAverageDetailCacheLatency(type: DetailType): Long {
        return averageLatency(cachedDetailLatencies[type])
    }

    fun getAverageDetailFreshLatency(type: DetailType): Long {
        return averageLatency(freshDetailLatencies[type])
    }

    fun getTrackLoadAverages(type: DetailType): TrackLoadAverages {
        synchronized(lock) {
            val samples = trackLoadSamples[type] ?: return TrackLoadAverages(null, null, null)
            if (samples.isEmpty()) return TrackLoadAverages(null, null, null)
            val small = mutableListOf<Long>()
            val medium = mutableListOf<Long>()
            val large = mutableListOf<Long>()
            for (sample in samples) {
                when {
                    sample.trackCount <= TRACK_BUCKET_SMALL -> small.add(sample.durationMs)
                    sample.trackCount <= TRACK_BUCKET_MEDIUM -> medium.add(sample.durationMs)
                    else -> large.add(sample.durationMs)
                }
            }
            return TrackLoadAverages(
                smallMs = averageOf(small),
                mediumMs = averageOf(medium),
                largeMs = averageOf(large)
            )
        }
    }

    private fun pruneStaleRequestsLocked(now: Long) {
        if (requestTimes.isEmpty()) return
        val iterator = requestTimes.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > REQUEST_TIMEOUT_MS) {
                iterator.remove()
            }
        }
    }

    private fun pruneDetailStartsLocked(now: Long) {
        if (detailStartTimes.isEmpty()) return
        val iterator = detailStartTimes.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > DETAIL_REQUEST_TIMEOUT_MS) {
                iterator.remove()
            }
        }
    }

    private fun recordDetailLatency(
        type: DetailType,
        key: String,
        latencies: ArrayDeque<Long>
    ) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val start = detailStartTimes[detailKey(type, key)] ?: return
            val duration = (now - start).coerceAtLeast(0)
            latencies.addLast(duration)
            while (latencies.size > DETAIL_LATENCY_WINDOW) {
                latencies.removeFirst()
            }
        }
    }

    private fun averageLatency(values: ArrayDeque<Long>?): Long {
        synchronized(lock) {
            if (values == null || values.isEmpty()) return 0L
            val sum = values.sum()
            return sum / values.size
        }
    }

    private fun detailKey(type: DetailType, key: String): String {
        return "${type.name}:${key.trim()}"
    }

    private inline fun <T> detailTypeMap(factory: () -> T): Map<DetailType, T> {
        return DetailType.values().associateWith { factory() }
    }

    private fun averageOf(values: List<Long>): Long? {
        if (values.isEmpty()) return null
        return values.sum() / values.size
    }

    data class TrackLoadAverages(
        val smallMs: Long?,
        val mediumMs: Long?,
        val largeMs: Long?
    )

    data class TrackLoadSample(
        val trackCount: Int,
        val durationMs: Long
    )

    enum class DetailType {
        PLAYLIST,
        ALBUM
    }

    enum class CacheType {
        PLAYLIST,
        ALBUM
    }

    private fun trimRequestMapToMaxLocked() {
        while (requestTimes.size > MAX_PENDING_REQUESTS) {
            val iterator = requestTimes.entries.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            } else {
                break
            }
        }
    }

    private companion object {
        private const val TAG = "PerformanceMonitor"
        private const val WINDOW_SIZE = 20
        private const val REQUEST_TIMEOUT_MS = 60_000L
        private const val MAX_PENDING_REQUESTS = 100
        private const val DETAIL_LATENCY_WINDOW = 20
        private const val TRACK_LOAD_WINDOW = 20
        private const val DETAIL_REQUEST_TIMEOUT_MS = 120_000L
        private const val TRACK_BUCKET_SMALL = 50
        private const val TRACK_BUCKET_MEDIUM = 150
        private val lock = Any()
    }
}
