package com.harmonixia.android.ui.components

import java.util.Locale

internal data class EqPresetValueTemplates(
    val frequencyKhzFormat: String,
    val frequencyHzFormat: String,
    val gainDbFormat: String,
    val qFormat: String
)

internal object EqPresetValueFormatter {
    fun formatFrequency(frequency: Double, templates: EqPresetValueTemplates): String {
        return if (frequency >= 1000) {
            val value = frequency / 1000.0
            String.format(Locale.US, templates.frequencyKhzFormat, value)
        } else {
            String.format(Locale.US, templates.frequencyHzFormat, frequency)
        }
    }

    fun formatGain(gain: Double, templates: EqPresetValueTemplates): String {
        val sign = if (gain > 0) "+" else ""
        return String.format(Locale.US, templates.gainDbFormat, sign, gain)
    }

    fun formatQ(q: Double, templates: EqPresetValueTemplates): String {
        return String.format(Locale.US, templates.qFormat, q)
    }
}
