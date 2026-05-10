package com.harmonixia.android.ui.screens.settings

import java.util.Locale

internal data class PerformanceSettingsMetricTemplates(
    val sizeMbFormat: String,
    val latencyMsFormat: String,
    val hitRatePercentFormat: String
)

internal object PerformanceSettingsMetricFormatter {
    fun formatBytes(value: Long?, templates: PerformanceSettingsMetricTemplates): String {
        val bytes = value ?: 0L
        val mb = bytes / (1024f * 1024f)
        return String.format(Locale.getDefault(), templates.sizeMbFormat, mb)
    }

    fun formatLatency(
        value: Long,
        unavailableLabel: String,
        templates: PerformanceSettingsMetricTemplates
    ): String {
        return if (value <= 0L) unavailableLabel else String.format(
            Locale.getDefault(),
            templates.latencyMsFormat,
            value
        )
    }

    fun formatOptionalLatency(
        value: Long?,
        unavailableLabel: String,
        templates: PerformanceSettingsMetricTemplates
    ): String {
        return value?.let {
            String.format(Locale.getDefault(), templates.latencyMsFormat, it)
        } ?: unavailableLabel
    }

    fun formatHitRate(
        value: Int?,
        unavailableLabel: String,
        templates: PerformanceSettingsMetricTemplates
    ): String {
        return value?.let {
            String.format(Locale.getDefault(), templates.hitRatePercentFormat, it)
        } ?: unavailableLabel
    }
}
