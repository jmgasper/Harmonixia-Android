package com.harmonixia.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.harmonixia.android.R
import kotlin.math.roundToLong

@Composable
fun SeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val safeDuration = duration.coerceAtLeast(0L)
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
            enabled = enabled && safeDuration > 0L,
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.semantics {
                contentDescription = seekLabel
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(displayedPosition),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(safeDuration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return stringResource(R.string.playback_time_format, minutes, seconds)
}
