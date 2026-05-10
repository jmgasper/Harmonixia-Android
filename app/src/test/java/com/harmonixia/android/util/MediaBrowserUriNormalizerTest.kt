package com.harmonixia.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaBrowserUriNormalizerTest {

    @Test
    fun resolveAutoArtworkUrl_blankValue_returnsNull() {
        val normalized = resolveAutoArtworkUrl("   ")

        assertNull(normalized)
    }

    @Test
    fun resolveAutoArtworkUrl_nonProxyUrl_returnsTrimmedOriginal() {
        val normalized = resolveAutoArtworkUrl("  https://example.com/artwork.jpg  ")

        assertEquals("https://example.com/artwork.jpg", normalized)
    }

    @Test
    fun resolveAutoArtworkUrl_imageProxyWithHttpPath_returnsPathValue() {
        val normalized = resolveAutoArtworkUrl(
            "https://example.com/imageproxy?path=https%3A%2F%2Fcdn.example.com%2Fcover.jpg"
        )

        assertEquals("https://cdn.example.com/cover.jpg", normalized)
    }

    @Test
    fun resolveAutoArtworkUrl_builtinProviderWithRelativePath_buildsAbsolutePath() {
        val normalized = resolveAutoArtworkUrl(
            "https://example.com/imageproxy?provider=builtin&path=%2Falbums%2Fcover.jpg"
        )

        assertEquals("https://example.com/albums/cover.jpg", normalized)
    }

    @Test
    fun resolveAutoArtworkUrl_nonBuiltinRelativePath_preservesOriginal() {
        val raw = "https://example.com/imageproxy?provider=spotify&path=%2Falbums%2Fcover.jpg"
        val normalized = resolveAutoArtworkUrl(raw)

        assertEquals(raw, normalized)
    }

    @Test
    fun resolveAutoArtworkUrl_invalidUri_preservesTrimmedOriginal() {
        val normalized = resolveAutoArtworkUrl("  https://example.com/invalid[uri  ")

        assertEquals("https://example.com/invalid[uri", normalized)
    }
}
