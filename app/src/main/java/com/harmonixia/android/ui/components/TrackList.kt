package com.harmonixia.android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Track
import java.util.Locale

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TrackList(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    headerContent: (LazyListScope.() -> Unit)? = null,
    showContextMenu: Boolean = false,
    isEditable: Boolean = false,
    onTrackLongClick: ((Track, Int) -> Unit)? = null,
    onAddToPlaylist: ((Track) -> Unit)? = null,
    onRemoveFromPlaylist: ((Track, Int) -> Unit)? = null,
    trackTitleTextStyle: TextStyle? = null,
    trackSupportingTextStyle: TextStyle? = null,
    trackMetadataTextStyle: TextStyle? = null,
    indexProvider: ((Track, Int) -> Int)? = null,
    showEmptyState: Boolean = true
) {
    var contextMenuTrackId by remember { mutableStateOf<String?>(null) }
    var contextMenuIndex by remember { mutableStateOf(-1) }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        headerContent?.invoke(this)
        if (tracks.isEmpty()) {
            if (showEmptyState) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.album_detail_no_tracks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            }
        } else {
            itemsIndexed(
                items = tracks,
                key = { _, track -> track.itemId }
            ) { index, track ->
                val effectiveIndex = indexProvider?.invoke(track, index) ?: index
                val displayNumber = if (track.trackNumber > 0) {
                    track.trackNumber
                } else {
                    effectiveIndex + 1
                }
                val hasLongClick = showContextMenu || onTrackLongClick != null
                val interactionModifier = if (hasLongClick) {
                    Modifier.combinedClickable(
                        onClick = { onTrackClick(track) },
                        onLongClick = {
                            onTrackLongClick?.invoke(track, effectiveIndex)
                            if (showContextMenu) {
                                contextMenuTrackId = track.itemId
                                contextMenuIndex = effectiveIndex
                            }
                        }
                    )
                } else {
                    Modifier.clickable(onClick = { onTrackClick(track) })
                }
                Box {
                    TrackListItem(
                        track = track,
                        trackNumber = displayNumber,
                        titleTextStyle = trackTitleTextStyle,
                        supportingTextStyle = trackSupportingTextStyle,
                        metadataTextStyle = trackMetadataTextStyle,
                        modifier = interactionModifier
                    )
                    if (showContextMenu && contextMenuTrackId == track.itemId) {
                        TrackContextMenu(
                            expanded = true,
                            onDismissRequest = {
                                contextMenuTrackId = null
                                contextMenuIndex = -1
                            },
                            isEditable = isEditable,
                            onPlay = { onTrackClick(track) },
                            onAddToPlaylist = { onAddToPlaylist?.invoke(track) },
                            onRemoveFromPlaylist = {
                                onRemoveFromPlaylist?.invoke(track, contextMenuIndex)
                            }
                        )
                    }
                }
                if (index < tracks.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}

@Composable
private fun TrackListItem(
    track: Track,
    trackNumber: Int,
    titleTextStyle: TextStyle?,
    supportingTextStyle: TextStyle?,
    metadataTextStyle: TextStyle?,
    modifier: Modifier = Modifier
) {
    val durationText = formatDuration(track.lengthSeconds)
    val qualityLabel = qualityDetailLabel(track.quality)
        ?: qualityLabelRes(track.quality)?.let { stringResource(it) }
    val title = if (track.title.isNotBlank()) track.title else track.uri
    val artist = if (track.artist.isNotBlank()) track.artist else track.album

    ListItem(
        modifier = modifier,
        leadingContent = {
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = trackNumber.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        },
        headlineContent = {
            if (titleTextStyle != null) {
                Text(
                    text = title,
                    style = titleTextStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        supportingContent = {
            if (supportingTextStyle != null) {
                Text(
                    text = artist,
                    style = supportingTextStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                if (qualityLabel != null) {
                    TrackQualityBadge(text = qualityLabel)
                }
                Text(
                    text = durationText,
                    style = metadataTextStyle ?: MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun TrackQualityBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.padding(bottom = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

private fun qualityLabelRes(quality: String?): Int? {
    val normalized = quality?.lowercase()?.trim().orEmpty()
    return when {
        normalized.contains("hi_res") ||
            normalized.contains("hi-res") ||
            normalized.contains("hires") ||
            normalized.contains("hi res") ->
            R.string.track_quality_hires
        normalized.contains("lossless") -> R.string.track_quality_lossless
        else -> null
    }
}

private fun qualityDetailLabel(quality: String?): String? {
    val normalized = quality?.lowercase()?.trim().orEmpty()
    if (normalized.isBlank()) return null
    val sampleRateKhz = parseSampleRateKhz(normalized)
    val bitrateKbps = parseBitrateKbps(normalized)
    return when {
        isLosslessQuality(normalized) -> sampleRateKhz?.let { formatKhz(it) }
            ?: bitrateKbps?.let { formatKbps(it) }
        isLossyQuality(normalized) -> bitrateKbps?.let { formatKbps(it) }
            ?: sampleRateKhz?.let { formatKhz(it) }
        sampleRateKhz != null -> formatKhz(sampleRateKhz)
        bitrateKbps != null -> formatKbps(bitrateKbps)
        else -> null
    }
}

private fun parseSampleRateKhz(quality: String): Double? {
    khzRegex.find(quality)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let { return it }
    hzRegex.find(quality)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let { return it / 1000.0 }
    val match = bitDepthSampleRateRegex.find(quality)
    val bitDepth = match?.groupValues?.getOrNull(1)?.toIntOrNull()
    val sampleRate = match?.groupValues?.getOrNull(2)?.toDoubleOrNull()
    if (bitDepth != null && bitDepth <= 32 && sampleRate != null && sampleRate >= 30) {
        return sampleRate
    }
    return null
}

private fun parseBitrateKbps(quality: String): Double? {
    return bitrateRegex.find(quality)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
}

private fun formatKhz(value: Double): String {
    val rounded = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
    return "$rounded kHz"
}

private fun formatKbps(value: Double): String {
    val rounded = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
    return "$rounded kbps"
}

private fun isLosslessQuality(quality: String): Boolean {
    return quality.contains("lossless") ||
        quality.contains("hi_res") ||
        quality.contains("hi-res") ||
        quality.contains("hires") ||
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
private val bitDepthSampleRateRegex = Regex(
    """\b(\d{2})\s*[/x]\s*(\d{2,3}(?:\.\d)?)\b""",
    RegexOption.IGNORE_CASE
)
