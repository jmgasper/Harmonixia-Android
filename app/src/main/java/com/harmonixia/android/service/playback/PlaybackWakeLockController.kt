package com.harmonixia.android.service.playback

import android.os.PowerManager

internal class PlaybackWakeLockController(
    private val wakeLock: PowerManager.WakeLock,
    private val timeoutMs: Long = DEFAULT_WAKE_LOCK_TIMEOUT_MS
) {
    fun acquireIfNeeded() {
        if (!wakeLock.isHeld) {
            wakeLock.acquire(timeoutMs)
        }
    }

    fun releaseIfHeld() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    companion object {
        private const val WAKE_LOCK_TAG = "Harmonixia:PlaybackWakeLock"
        const val DEFAULT_WAKE_LOCK_TIMEOUT_MS = 24 * 60 * 60 * 1000L

        fun create(powerManager: PowerManager): PlaybackWakeLockController {
            val wakeLock = powerManager
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                .apply { setReferenceCounted(false) }
            return PlaybackWakeLockController(wakeLock)
        }
    }
}
