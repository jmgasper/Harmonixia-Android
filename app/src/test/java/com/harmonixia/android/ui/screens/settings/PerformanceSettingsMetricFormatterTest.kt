package com.harmonixia.android.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceSettingsMetricFormatterTest {

    private val templates = PerformanceSettingsMetricTemplates(
        sizeMbFormat = "%1$.1f MBX",
        latencyMsFormat = "%1\$d msX"
    )

    @Test
    fun formatBytes_formatsMegabytesWithSingleDecimal() {
        assertEquals("1.0 MBX", PerformanceSettingsMetricFormatter.formatBytes(1_048_576L, templates))
        assertEquals("0.0 MBX", PerformanceSettingsMetricFormatter.formatBytes(null, templates))
    }

    @Test
    fun formatLatency_returnsUnavailableForNonPositive() {
        assertEquals("N/A", PerformanceSettingsMetricFormatter.formatLatency(0L, "N/A", templates))
        assertEquals("N/A", PerformanceSettingsMetricFormatter.formatLatency(-3L, "N/A", templates))
    }

    @Test
    fun formatLatency_usesTemplateForPositiveValues() {
        assertEquals("87 msX", PerformanceSettingsMetricFormatter.formatLatency(87L, "N/A", templates))
    }

    @Test
    fun formatOptionalLatency_usesTemplateWhenPresent() {
        assertEquals("42 msX", PerformanceSettingsMetricFormatter.formatOptionalLatency(42L, "N/A", templates))
    }

    @Test
    fun formatOptionalLatency_returnsUnavailableWhenNull() {
        assertEquals("N/A", PerformanceSettingsMetricFormatter.formatOptionalLatency(null, "N/A", templates))
    }
}
