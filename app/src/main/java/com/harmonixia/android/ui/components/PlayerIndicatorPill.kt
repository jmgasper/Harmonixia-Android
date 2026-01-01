package com.harmonixia.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speaker
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
import com.harmonixia.android.util.PlayerSelection

@Composable
fun PlayerIndicatorPill(
    selectedPlayer: Player?,
    localPlayerId: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when {
        selectedPlayer == null -> stringResource(R.string.player_indicator_no_player)
        PlayerSelection.isLocalPlayer(selectedPlayer, localPlayerId) ->
            stringResource(R.string.player_selection_this_device)
        else -> selectedPlayer.name
    }
    val indicatorContentDescription = stringResource(R.string.content_desc_player_indicator)
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
            Icon(
                imageVector = Icons.Outlined.Speaker,
                contentDescription = null
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
