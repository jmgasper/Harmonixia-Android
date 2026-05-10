package com.harmonixia.android.ui.components

import java.util.Locale

internal object TrackDurationFormatter {
    fun formatDuration(seconds: Int, minuteSecondFormat: String): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val remainingSeconds = safeSeconds % 60
        return String.format(Locale.getDefault(), minuteSecondFormat, minutes, remainingSeconds)
    }
}
