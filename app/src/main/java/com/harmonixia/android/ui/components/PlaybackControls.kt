package com.harmonixia.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.RepeatMode

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    hasNext: Boolean,
    hasPrevious: Boolean,
    repeatMode: RepeatMode,
    shuffle: Boolean,
    isRepeatModeUpdating: Boolean,
    isShuffleUpdating: Boolean,
    onRepeatToggle: () -> Unit,
    onShuffleToggle: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val activeTint = MaterialTheme.colorScheme.onSurface
    val repeatTint = if (repeatMode == RepeatMode.OFF) inactiveTint else activeTint
    val shuffleTint = if (shuffle) activeTint else inactiveTint
    val controlButtonSize = 48.dp
    val spinnerSize = 20.dp
    val repeatIcon = when (repeatMode) {
        RepeatMode.OFF -> Icons.Filled.Repeat
        RepeatMode.ALL -> Icons.Filled.Repeat
        RepeatMode.ONE -> Icons.Filled.RepeatOne
    }
    val repeatDescription = stringResource(
        when (repeatMode) {
            RepeatMode.OFF -> R.string.action_repeat_off
            RepeatMode.ALL -> R.string.action_repeat_all
            RepeatMode.ONE -> R.string.action_repeat_one
        }
    )
    val shuffleDescription = stringResource(
        if (shuffle) R.string.action_shuffle_on else R.string.action_shuffle_off
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(controlButtonSize),
            contentAlignment = Alignment.Center
        ) {
            if (isRepeatModeUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(spinnerSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRepeatToggle()
                    },
                    enabled = enabled,
                    modifier = Modifier.size(controlButtonSize)
                ) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = repeatDescription,
                        tint = repeatTint
                    )
                }
            }
        }
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPrevious()
            },
            enabled = enabled && hasPrevious
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(R.string.action_previous_track)
            )
        }
        FilledTonalIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPlayPause()
            },
            enabled = enabled,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) {
                    Icons.Filled.Pause
                } else {
                    Icons.Filled.PlayArrow
                },
                contentDescription = stringResource(
                    if (isPlaying) {
                        R.string.action_pause
                    } else {
                        R.string.action_play
                    }
                )
            )
        }
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNext()
            },
            enabled = enabled && hasNext
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.action_next_track)
            )
        }
        Box(
            modifier = Modifier.size(controlButtonSize),
            contentAlignment = Alignment.Center
        ) {
            if (isShuffleUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(spinnerSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onShuffleToggle()
                    },
                    enabled = enabled,
                    modifier = Modifier.size(controlButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = shuffleDescription,
                        tint = shuffleTint
                    )
                }
            }
        }
    }
}
