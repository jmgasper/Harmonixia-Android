package com.harmonixia.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.ui.theme.ExternalPlaybackGreen
import com.harmonixia.android.ui.theme.ExternalPlaybackOnGreen
import com.harmonixia.android.util.PlayerSelection

@Composable
fun PlayerIndicatorPill(
    selectedPlayer: Player?,
    localPlayerId: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExternalPlayback = selectedPlayer != null &&
        !PlayerSelection.isLocalPlayer(selectedPlayer, localPlayerId)
    val label = when {
        selectedPlayer == null -> stringResource(R.string.player_indicator_no_player)
        PlayerSelection.isLocalPlayer(selectedPlayer, localPlayerId) ->
            stringResource(R.string.player_selection_this_device)
        else -> selectedPlayer.name
    }
    val indicatorContentDescription = stringResource(R.string.content_desc_player_indicator)
    val containerColor = if (isExternalPlayback) {
        ExternalPlaybackGreen
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isExternalPlayback) {
        ExternalPlaybackOnGreen
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = indicatorContentDescription
            }
            .clickable(onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Speaker,
                    contentDescription = null
                )
                if (isExternalPlayback) {
                    Icon(
                        imageVector = Icons.Outlined.Wifi,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
