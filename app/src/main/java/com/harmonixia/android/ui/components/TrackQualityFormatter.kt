package com.harmonixia.android.ui.components

import com.harmonixia.android.R
import java.util.Locale

fun formatTrackQualityLabel(
    quality: String?,
    resolveLabel: (Int) -> String,
    showLosslessDetail: Boolean = true
): String? {
    val raw = quality?.trim().orEmpty()
    val normalized = raw.lowercase().trim()
    if (normalized.isBlank()) return null
    val labelRes = qualityLabelRes(normalized)
    val label = labelRes?.let(resolveLabel)
    val detail = if (!showLosslessDetail && labelRes == R.string.track_quality_lossless) {
        null
    } else {
        qualityDetailLabel(normalized, resolveLabel)
    }
    val resolved = when {
        label != null && detail != null -> formatQualityTemplate(
            R.string.track_quality_label_with_detail_format,
            resolveLabel,
            label,
            detail
        )
        label != null -> label
        detail != null -> detail
        else -> null
    }
    return resolved ?: raw.takeIf { it.isNotBlank() }
}

enum class TrackQualityCategory {
    LOSSLESS,
    LOSSY,
    UNKNOWN
}

fun trackQualityCategory(quality: String?): TrackQualityCategory? {
    val normalized = quality?.trim()?.lowercase().orEmpty()
    if (normalized.isBlank()) return null
    return when {
        isLosslessQuality(normalized) -> TrackQualityCategory.LOSSLESS
        isLossyQuality(normalized) -> TrackQualityCategory.LOSSY
        else -> TrackQualityCategory.UNKNOWN
    }
}

private fun qualityLabelRes(normalized: String): Int? {
    return when {
        isHiResQuality(normalized) -> R.string.track_quality_hires
        isLosslessQuality(normalized) -> R.string.track_quality_lossless
        else -> null
    }
}

private fun qualityDetailLabel(normalized: String, resolveLabel: (Int) -> String): String? {
    val sampleRateKhz = parseSampleRateKhz(normalized)
    val bitDepth = parseBitDepth(normalized)
    val bitrateKbps = parseBitrateKbps(normalized)
    val sampleRateDetail = formatSampleRateBitDepth(sampleRateKhz, bitDepth, resolveLabel)
    return when {
        isLosslessQuality(normalized) -> sampleRateDetail ?: bitrateKbps?.let { formatKbps(it, resolveLabel) }
        isLossyQuality(normalized) -> bitrateKbps?.let { formatKbps(it, resolveLabel) } ?: sampleRateDetail
        sampleRateDetail != null -> sampleRateDetail
        bitrateKbps != null -> formatKbps(bitrateKbps, resolveLabel)
        else -> null
    }
}

private fun formatSampleRateBitDepth(
    sampleRateKhz: Double?,
    bitDepth: Int?,
    resolveLabel: (Int) -> String
): String? {
    val bitUnit = resolveLabel(R.string.track_quality_unit_bit)
    return when {
        sampleRateKhz != null && bitDepth != null -> formatQualityTemplate(
            R.string.track_quality_detail_sample_rate_bit_depth_format,
            resolveLabel,
            formatKhz(sampleRateKhz, resolveLabel),
            bitDepth,
            bitUnit
        )
        sampleRateKhz != null -> formatKhz(sampleRateKhz, resolveLabel)
        bitDepth != null -> formatQualityTemplate(
            R.string.track_quality_detail_bit_depth_format,
            resolveLabel,
            bitDepth,
            bitUnit
        )
        else -> null
    }
}

private fun parseSampleRateKhz(quality: String): Double? {
    khzRegex.find(quality)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let { return it }
    hzRegex.find(quality)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let { return it / 1000.0 }
    val match = bitDepthSampleRateRegex.find(quality)
    val sampleRate = match?.groupValues?.getOrNull(2)?.toDoubleOrNull()
    val unit = match?.groupValues?.getOrNull(3)
    if (sampleRate != null && sampleRate >= 30) {
        val isHz = unit?.equals("hz", ignoreCase = true) == true
        val isKhz = unit?.contains("khz", ignoreCase = true) == true
        return when {
            isKhz -> sampleRate
            isHz || sampleRate >= 1000 -> sampleRate / 1000.0
            else -> sampleRate
        }
    }
    return null
}

private fun parseBitDepth(quality: String): Int? {
    bitDepthRegex.find(quality)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
    val match = bitDepthSampleRateRegex.find(quality)
    val bitDepth = match?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (bitDepth != null && bitDepth <= 32) {
        return bitDepth
    }
    return null
}

private fun parseBitrateKbps(quality: String): Double? {
    return bitrateRegex.find(quality)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
}

private fun formatKhz(value: Double, resolveLabel: (Int) -> String): String {
    val rounded = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
    val unit = resolveLabel(R.string.track_quality_unit_khz)
    return formatQualityTemplate(
        R.string.track_quality_detail_khz_format,
        resolveLabel,
        rounded,
        unit
    )
}

private fun formatKbps(value: Double, resolveLabel: (Int) -> String): String {
    val rounded = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
    val unit = resolveLabel(R.string.track_quality_unit_kbps)
    return formatQualityTemplate(
        R.string.track_quality_detail_kbps_format,
        resolveLabel,
        rounded,
        unit
    )
}

private fun formatQualityTemplate(
    templateRes: Int,
    resolveLabel: (Int) -> String,
    vararg args: Any
): String {
    return String.format(Locale.getDefault(), resolveLabel(templateRes), *args)
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

private fun isLossyQuality(quality: String): Boolean {
    return quality.contains("mp3") ||
        quality.contains("aac") ||
        quality.contains("ogg") ||
        quality.contains("opus") ||
        quality.contains("vorbis") ||
        quality.contains("wma") ||
        quality.contains("m4a")
}

private val khzRegex = Regex("""(\d+(?:\.\d+)?)\s*k\s*hz""", RegexOption.IGNORE_CASE)
private val hzRegex = Regex("""(\d{4,6})\s*hz""", RegexOption.IGNORE_CASE)
private val bitrateRegex = Regex(
    """(\d+(?:\.\d+)?)\s*(kbps|kbit/s|kbits/s|kb/s)""",
    RegexOption.IGNORE_CASE
)
private val bitDepthRegex = Regex("""\b(\d{2})\s*[- ]?bit(?:s)?\b""", RegexOption.IGNORE_CASE)
private val bitDepthSampleRateRegex = Regex(
    """\b(\d{2})\s*[/x]\s*(\d{2,6}(?:\.\d+)?)\s*(k?hz)?\b""",
    RegexOption.IGNORE_CASE
)
