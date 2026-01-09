package com.harmonixia.android.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.PlaybackState
import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.ui.theme.rememberAdaptiveSpacing
import com.harmonixia.android.util.PlayerSelection
import kotlin.math.roundToInt

@Composable
fun PlayerSelectionDialog(
    players: List<Player>,
    selectedPlayer: Player?,
    localPlayerId: String? = null,
    onPlayerSelected: (Player) -> Unit,
    onPlayerVolumeChange: (Player, Int) -> Unit,
    onPlayerMuteChange: (Player, Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdaptiveSpacing()
    val volumePlayer = remember(players, selectedPlayer) {
        val selectedMatch = selectedPlayer?.let { selected ->
            players.firstOrNull { it.playerId == selected.playerId } ?: selected
        }
        selectedMatch ?: players.firstOrNull { it.playbackState == PlaybackState.PLAYING }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.extraLarge, vertical = spacing.large)
                ) {
                    Text(
                        text = stringResource(R.string.player_selection_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.content_desc_close)
                        )
                    }
                }

                if (players.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(
                            text = "No players available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = spacing.extraLarge)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = spacing.extraLarge,
                            vertical = spacing.medium
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.large),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(players, key = { it.playerId }) { player ->
                            val isSelected = selectedPlayer?.playerId == player.playerId
                            val isAvailable = player.available
                            val isLocal = PlayerSelection.isLocalPlayer(player, localPlayerId)
                            val nameColor = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isAvailable -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                            val statusColor = when {
                                !isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val iconTint = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                            val containerColor = when {
                                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                isAvailable -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                            Surface(
                                color = containerColor,
                                tonalElevation = if (isSelected) 2.dp else 0.dp,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onPlayerSelected(player)
                                    onDismiss()
                                },
                                enabled = isAvailable
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = player.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = nameColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isLocal) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    shape = MaterialTheme.shapes.small
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.player_selection_this_device),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    supportingContent = {
                                        Text(
                                            text = if (isAvailable) {
                                                stringResource(R.string.player_available)
                                            } else {
                                                stringResource(R.string.player_unavailable)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Outlined.Speaker,
                                            contentDescription = stringResource(R.string.content_desc_player_icon),
                                            tint = iconTint
                                        )
                                    },
                                    trailingContent = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }

                val showVolumeSection = players.isNotEmpty() &&
                    volumePlayer != null &&
                    !PlayerSelection.isLocalPlayer(volumePlayer, localPlayerId)
                if (showVolumeSection) {
                    HorizontalDivider()
                    PlayerVolumeSection(
                        player = volumePlayer,
                        localPlayerId = localPlayerId,
                        onPlayerVolumeChange = onPlayerVolumeChange,
                        onPlayerMuteChange = onPlayerMuteChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.extraLarge, vertical = spacing.large)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerVolumeSection(
    player: Player,
    localPlayerId: String?,
    onPlayerVolumeChange: (Player, Int) -> Unit,
    onPlayerMuteChange: (Player, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMuted = player.volumeMuted == true
    val isAvailable = player.available
    val playerName = if (PlayerSelection.isLocalPlayer(player, localPlayerId)) {
        stringResource(R.string.player_selection_this_device)
    } else {
        player.name
    }
    val volumeLabel = stringResource(R.string.label_volume)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = volumeLabel,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = playerName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }
        PlayerVolumeBar(
            volume = player.volume,
            isMuted = isMuted,
            enabled = isAvailable,
            onVolumeChange = { volume ->
                onPlayerVolumeChange(player, volume)
            },
            onMuteToggle = { muted ->
                onPlayerMuteChange(player, muted)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerVolumeBar(
    volume: Int,
    isMuted: Boolean,
    enabled: Boolean,
    onVolumeChange: (Int) -> Unit,
    onMuteToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val clampedVolume = volume.coerceIn(0, MAX_PLAYER_VOLUME)
    val volumeLabel = stringResource(R.string.label_volume)
    val iconTint = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    val sliderColors = SliderDefaults.colors(
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
    )
    val interactionSource = remember { MutableInteractionSource() }
    val controlHeight = 48.dp
    val thumbSize = DpSize(20.dp, 20.dp)

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides controlHeight
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { onMuteToggle(!isMuted) },
                enabled = enabled,
                modifier = Modifier.size(controlHeight)
            ) {
                val muteLabel = stringResource(
                    if (isMuted) R.string.content_desc_unmute else R.string.content_desc_mute
                )
                Icon(
                    imageVector = Icons.Outlined.VolumeOff,
                    contentDescription = muteLabel,
                    tint = if (isMuted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        iconTint
                    }
                )
            }
            Slider(
                value = clampedVolume.toFloat(),
                onValueChange = { value ->
                    onVolumeChange(value.roundToInt().coerceIn(0, MAX_PLAYER_VOLUME))
                },
                valueRange = 0f..MAX_PLAYER_VOLUME.toFloat(),
                steps = 0,
                enabled = enabled,
                colors = sliderColors,
                interactionSource = interactionSource,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        colors = sliderColors,
                        enabled = enabled,
                        thumbSize = thumbSize
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        colors = sliderColors,
                        enabled = enabled,
                        sliderState = sliderState
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(controlHeight)
                    .padding(horizontal = 12.dp)
                    .semantics { contentDescription = volumeLabel }
            )
            Icon(
                imageVector = Icons.Outlined.VolumeUp,
                contentDescription = stringResource(R.string.content_desc_volume_max),
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private const val MAX_PLAYER_VOLUME = 100
