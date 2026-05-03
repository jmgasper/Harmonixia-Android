package com.harmonixia.android.service.playback

import android.os.PowerManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PlaybackWakeLockControllerTest {

    private val wakeLock = mockk<PowerManager.WakeLock>(relaxed = true)

    @Test
    fun acquireIfNeeded_whenNotHeld_acquiresWithDefaultTimeout() {
        every { wakeLock.isHeld } returns false

        PlaybackWakeLockController(wakeLock).acquireIfNeeded()

        verify(exactly = 1) {
            wakeLock.acquire(PlaybackWakeLockController.DEFAULT_WAKE_LOCK_TIMEOUT_MS)
        }
    }

    @Test
    fun acquireIfNeeded_whenAlreadyHeld_doesNotAcquireAgain() {
        every { wakeLock.isHeld } returns true

        PlaybackWakeLockController(wakeLock).acquireIfNeeded()

        verify(exactly = 0) { wakeLock.acquire(any<Long>()) }
    }

    @Test
    fun releaseIfHeld_whenHeld_releasesWakeLock() {
        every { wakeLock.isHeld } returns true

        PlaybackWakeLockController(wakeLock).releaseIfHeld()

        verify(exactly = 1) { wakeLock.release() }
    }

    @Test
    fun releaseIfHeld_whenNotHeld_doesNotReleaseWakeLock() {
        every { wakeLock.isHeld } returns false

        PlaybackWakeLockController(wakeLock).releaseIfHeld()

        verify(exactly = 0) { wakeLock.release() }
    }
}
