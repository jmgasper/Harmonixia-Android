package com.harmonixia.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    enabled: Boolean = true,
    centerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val safeDuration = duration.coerceAtLeast(0L)
    val isEnabled = enabled && safeDuration > 0L
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val progress by remember(currentPosition, safeDuration) {
        derivedStateOf {
            if (safeDuration > 0L) {
                (currentPosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    LaunchedEffect(progress, isDragging) {
        if (!isDragging) {
            sliderPosition = progress
        }
    }

    val displayedPosition = if (isDragging) {
        (sliderPosition * safeDuration).roundToLong()
    } else {
        currentPosition.coerceAtLeast(0L)
    }
    val seekLabel = stringResource(R.string.label_seek)
    val interactionSource = remember { MutableInteractionSource() }
    val inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val activeTrackColor = MaterialTheme.colorScheme.primary
    val timeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    val sliderColors = SliderDefaults.colors(
        thumbColor = activeTrackColor,
        activeTrackColor = activeTrackColor,
        inactiveTrackColor = inactiveTrackColor
    )

    Column(modifier = modifier) {
        Slider(
            value = sliderPosition,
            onValueChange = { value ->
                isDragging = true
                sliderPosition = value
            },
            onValueChangeFinished = {
                isDragging = false
                if (safeDuration > 0L) {
                    onSeek((sliderPosition * safeDuration).roundToLong())
                }
            },
            valueRange = 0f..1f,
            enabled = isEnabled,
            colors = sliderColors,
            interactionSource = interactionSource,
            thumb = {
                SeekBarThumb(
                    enabled = isEnabled,
                    color = activeTrackColor
                )
            },
            track = { sliderState ->
                SeekBarTrack(
                    sliderState = sliderState,
                    activeColor = activeTrackColor,
                    inactiveColor = inactiveTrackColor,
                    enabled = isEnabled
                )
            },
            modifier = Modifier.semantics {
                contentDescription = seekLabel
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = formatTime(displayedPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = timeColor
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                centerContent?.invoke()
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = formatTime(safeDuration),
                    style = MaterialTheme.typography.labelSmall,
                    color = timeColor
                )
            }
        }
    }
}

@Composable
private fun SeekBarThumb(
    enabled: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val thumbColor = if (enabled) color else color.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .size(SeekBarThumbSize)
            .background(
                color = thumbColor,
                shape = CircleShape
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBarTrack(
    sliderState: SliderState,
    activeColor: Color,
    inactiveColor: Color,
    enabled: Boolean
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val active = if (enabled) activeColor else activeColor.copy(alpha = 0.4f)
    val inactive = if (enabled) inactiveColor else inactiveColor.copy(alpha = 0.2f)
    val valueRange = sliderState.valueRange
    val fraction = if (valueRange.endInclusive > valueRange.start) {
        val coercedValue = sliderState.value.coerceIn(valueRange.start, valueRange.endInclusive)
        ((coercedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start))
            .coerceIn(0f, 1f)
    } else {
        0f
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(SeekBarTrackHeight)
    ) {
        val sliderLeft = Offset(0f, center.y)
        val sliderRight = Offset(size.width, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight
        val strokeWidth = SeekBarTrackHeight.toPx()
        drawLine(
            color = inactive,
            start = sliderStart,
            end = sliderEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        val activeEnd = Offset(
            x = sliderStart.x + (sliderEnd.x - sliderStart.x) * fraction,
            y = center.y
        )
        drawLine(
            color = active,
            start = sliderStart,
            end = activeEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private val SeekBarThumbSize = DpSize(20.dp, 20.dp)
private val SeekBarTrackHeight = 4.dp

@Composable
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return stringResource(R.string.playback_time_format, minutes, seconds)
}
