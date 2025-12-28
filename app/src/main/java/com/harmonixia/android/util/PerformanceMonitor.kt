package com.harmonixia.android.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitor @Inject constructor() {
    private val requestTimes = LinkedHashMap<String, Long>()
    private val latencyWindow = ArrayDeque<Long>(WINDOW_SIZE)

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
        private val lock = Any()
    }
}
