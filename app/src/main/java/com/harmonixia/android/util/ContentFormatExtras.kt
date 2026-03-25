package com.harmonixia.android.util

import android.os.Bundle
import com.harmonixia.android.BuildConfig
import com.harmonixia.android.R

fun Bundle.applyContentFormatIcons(quality: String?) {
    val normalized = quality?.trim()?.lowercase().orEmpty()
    val iconRes = when {
        isHiResQuality(normalized) -> R.drawable.ic_content_format_hires
        isLosslessQuality(normalized) -> R.drawable.ic_content_format_lossless
        hasBitrate(normalized) -> R.drawable.ic_content_format_bitrate
        else -> null
    }
    if (iconRes == null) {
        remove(EXTRA_CONTENT_FORMAT_TINTABLE_LARGE_ICON_URI)
        remove(EXTRA_CONTENT_FORMAT_TINTABLE_SMALL_ICON_URI)
        return
    }
    val uri = "android.resource://${BuildConfig.APPLICATION_ID}/$iconRes"
    putString(EXTRA_CONTENT_FORMAT_TINTABLE_LARGE_ICON_URI, uri)
    putString(EXTRA_CONTENT_FORMAT_TINTABLE_SMALL_ICON_URI, uri)
}

private fun isHiResQuality(quality: String): Boolean {
    return quality.contains("hi_res") ||
        quality.contains("hi-res") ||
        quality.contains("hires") ||
        quality.contains("hi res")
}

private fun isLosslessQuality(quality: String): Boolean {
    return quality.contains("lossless") ||
        isHiResQuality(quality) ||
        quality.contains("flac") ||
        quality.contains("alac") ||
        quality.contains("wav") ||
        quality.contains("aiff") ||
        quality.contains("pcm") ||
        quality.contains("dsd")
}

private fun hasBitrate(quality: String): Boolean {
    return bitrateRegex.containsMatchIn(quality)
}

private val bitrateRegex = Regex(
    """(\d+(?:\.\d+)?)\s*(kbps|kbit/s|kbits/s|kb/s)""",
    RegexOption.IGNORE_CASE
)
