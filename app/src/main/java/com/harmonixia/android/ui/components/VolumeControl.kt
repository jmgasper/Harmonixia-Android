package com.harmonixia.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R

@Composable
fun VolumeControl(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current
    val volumeIcon = when {
        volume <= 0f -> Icons.Filled.VolumeOff
        volume < 0.33f -> Icons.Filled.VolumeMute
        volume < 0.66f -> Icons.Filled.VolumeDown
        else -> Icons.Filled.VolumeUp
    }
    val volumeLabel = stringResource(R.string.label_volume)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { isExpanded = !isExpanded },
            enabled = enabled
        ) {
            Icon(
                imageVector = volumeIcon,
                contentDescription = volumeLabel
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.weight(1f)
        ) {
            Slider(
                value = volume.coerceIn(0f, 1f),
                onValueChange = { value ->
                    onVolumeChange(value.coerceIn(0f, 1f))
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                valueRange = 0f..1f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .semantics { contentDescription = volumeLabel }
            )
        }
    }
}
