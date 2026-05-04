package com.harmonixia.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class EqPresetValueFormatterTest {

    private val templates = EqPresetValueTemplates(
        frequencyKhzFormat = "%1$.1f kHzX",
        frequencyHzFormat = "%1$.0f HzX",
        gainDbFormat = "%1\$s%2$.1f dBX",
        qFormat = "%1$.2f"
    )

    @Test
    fun formatFrequency_usesKhzTemplateWhenFrequencyAtLeast1000() {
        assertEquals("2.0 kHzX", EqPresetValueFormatter.formatFrequency(2000.0, templates))
    }

    @Test
    fun formatFrequency_usesHzTemplateWhenFrequencyBelow1000() {
        assertEquals("80 HzX", EqPresetValueFormatter.formatFrequency(80.0, templates))
    }

    @Test
    fun formatGain_preservesPositiveNegativeAndZeroBehavior() {
        assertEquals("+3.5 dBX", EqPresetValueFormatter.formatGain(3.5, templates))
        assertEquals("-2.0 dBX", EqPresetValueFormatter.formatGain(-2.0, templates))
        assertEquals("0.0 dBX", EqPresetValueFormatter.formatGain(0.0, templates))
    }

    @Test
    fun formatQ_usesConfiguredPrecisionTemplate() {
        assertEquals("0.71", EqPresetValueFormatter.formatQ(0.707, templates))
    }
}
