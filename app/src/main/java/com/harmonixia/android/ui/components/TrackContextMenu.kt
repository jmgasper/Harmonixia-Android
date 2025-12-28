package com.harmonixia.android.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.harmonixia.android.R

@Composable
fun TrackContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isEditable: Boolean,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.track_action_play)) },
            onClick = {
                onPlay()
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.track_action_add_to_playlist)) },
            onClick = {
                onAddToPlaylist()
                onDismissRequest()
            }
        )
        if (isEditable) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.track_action_remove_from_playlist)) },
                onClick = {
                    onRemoveFromPlaylist()
                    onDismissRequest()
                }
            )
        }
    }
}
