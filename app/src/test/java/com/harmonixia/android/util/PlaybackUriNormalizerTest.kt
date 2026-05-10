package com.harmonixia.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackUriNormalizerTest {

    @Test
    fun normalizePlaybackUri_trimsWhitespace() {
        val normalized = normalizePlaybackUri("  https://example.com/track  ")

        assertEquals("https://example.com/track", normalized)
    }

    @Test
    fun normalizePlaybackUri_blankValue_returnsNull() {
        val normalized = normalizePlaybackUri("   ")

        assertNull(normalized)
    }

    @Test
    fun normalizePlaybackUriOrOriginal_blankValue_preservesOriginal() {
        val normalized = normalizePlaybackUriOrOriginal("   ")

        assertEquals("   ", normalized)
    }

    @Test
    fun resolvePlaybackStreamUri_prefersExtrasWhenPresent() {
        val resolved = resolvePlaybackStreamUri(
            extrasStreamUri = "  https://example.com/stream  ",
            localConfigurationUri = "https://example.com/local"
        )

        assertEquals("https://example.com/stream", resolved)
    }

    @Test
    fun resolvePlaybackStreamUri_fallsBackToLocalConfiguration() {
        val resolved = resolvePlaybackStreamUri(
            extrasStreamUri = "   ",
            localConfigurationUri = " content://media/external/audio/1 "
        )

        assertEquals("content://media/external/audio/1", resolved)
    }

    @Test
    fun resolvePlaybackStreamUri_blankValues_returnNull() {
        val resolved = resolvePlaybackStreamUri(
            extrasStreamUri = "   ",
            localConfigurationUri = "   "
        )

        assertNull(resolved)
    }

    @Test
    fun isSchemeLessPlaybackUri_localUnixPath_returnsTrue() {
        val localPath = isSchemeLessPlaybackUri(" /tmp/music/song.flac ")

        assertEquals(true, localPath)
    }

    @Test
    fun isSchemeLessPlaybackUri_remoteUrl_returnsFalse() {
        val remoteUrl = isSchemeLessPlaybackUri(" https://example.com/stream.mp3 ")

        assertEquals(false, remoteUrl)
    }

    @Test
    fun isSchemeLessPlaybackUri_windowsPath_returnsTrue() {
        val localPath = isSchemeLessPlaybackUri(" C:\\\\Music\\\\song.flac ")

        assertEquals(true, localPath)
    }
}
