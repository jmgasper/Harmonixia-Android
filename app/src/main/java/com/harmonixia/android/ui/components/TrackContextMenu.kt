package com.harmonixia.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
    onDownload: () -> Unit,
    isDownloaded: Boolean,
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
        if (!isDownloaded) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.track_action_download)) },
                onClick = {
                    onDownload()
                    onDismissRequest()
                }
            )
        } else {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.downloads_completed)) },
                onClick = {},
                enabled = false,
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null)
                }
            )
        }
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
