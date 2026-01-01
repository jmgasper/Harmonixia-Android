package com.harmonixia.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.util.PlayerSelection

@Composable
fun PlayerSelectionDialog(
    players: List<Player>,
    selectedPlayer: Player?,
    localPlayerId: String? = null,
    onPlayerSelected: (Player) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.player_selection_title)) },
        text = {
            if (players.isEmpty()) {
                Text(
                    text = "No players available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
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
                        ListItem(
                            headlineContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = player.name,
                                        style = MaterialTheme.typography.bodyMedium,
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
                                                text = "(${stringResource(R.string.player_selection_this_device)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                            trailingContent = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isAvailable) {
                                    onPlayerSelected(player)
                                    onDismiss()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_back))
            }
        },
        modifier = modifier
    )
}
