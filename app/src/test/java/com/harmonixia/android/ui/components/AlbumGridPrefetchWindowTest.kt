package com.harmonixia.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumGridPrefetchWindowTest {

    @Test
    fun computeArtworkPrefetchWindow_emptyList_returnsNull() {
        val result = computeArtworkPrefetchWindow(firstVisibleItemIndex = 0, itemCount = 0)

        assertNull(result)
    }

    @Test
    fun computeArtworkPrefetchWindow_defaultOffsets_returnsExpectedRange() {
        val result = computeArtworkPrefetchWindow(firstVisibleItemIndex = 0, itemCount = 100)

        assertEquals(20..60, result)
    }

    @Test
    fun computeArtworkPrefetchWindow_nearEnd_clampsToLastIndex() {
        val result = computeArtworkPrefetchWindow(firstVisibleItemIndex = 85, itemCount = 100)

        assertEquals(99..99, result)
    }

    @Test
    fun computeArtworkPrefetchWindow_customOffsetsWithInvertedBounds_returnsNull() {
        val result = computeArtworkPrefetchWindow(
            firstVisibleItemIndex = 10,
            itemCount = 100,
            startOffset = 60,
            endOffset = 20
        )

        assertNull(result)
    }

    @Test
    fun computeArtworkPrefetchWindow_negativeFirstVisibleIndex_clampsToZero() {
        val result = computeArtworkPrefetchWindow(firstVisibleItemIndex = -50, itemCount = 10)

        assertEquals(0..9, result)
    }
}
