package com.harmonixia.android.ui.components

import java.util.Locale

internal data class EqGraphLabelTemplates(
    val frequencyIntegerKFormat: String,
    val frequencyDecimalKFormat: String,
    val gainPositiveFormat: String,
    val gainDefaultFormat: String
)

internal object EqGraphLabelFormatter {
    fun formatFrequencyLabel(freq: Int, templates: EqGraphLabelTemplates): String {
        return if (freq >= 1000) {
            val value = freq / 1000.0
            if (value % 1.0 == 0.0) {
                String.format(Locale.US, templates.frequencyIntegerKFormat, value.toInt())
            } else {
                String.format(Locale.US, templates.frequencyDecimalKFormat, value)
            }
        } else {
            freq.toString()
        }
    }

    fun formatGainLabel(gain: Int, templates: EqGraphLabelTemplates): String {
        return if (gain > 0) {
            String.format(Locale.US, templates.gainPositiveFormat, gain)
        } else {
            String.format(Locale.US, templates.gainDefaultFormat, gain)
        }
    }
}
