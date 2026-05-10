package com.harmonixia.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackDurationFormatterTest {

    @Test
    fun formatDuration_zeroPadsSeconds() {
        assertEquals("2:05", TrackDurationFormatter.formatDuration(125, "%1\$d:%2\$02d"))
    }

    @Test
    fun formatDuration_clampsNegativeSecondsToZero() {
        assertEquals("0:00", TrackDurationFormatter.formatDuration(-15, "%1\$d:%2\$02d"))
    }

    @Test
    fun formatDuration_supportsLocalizedTemplateLayout() {
        assertEquals(
            "3 m 07 s",
            TrackDurationFormatter.formatDuration(187, "%1\$d m %2\$02d s")
        )
    }
}
