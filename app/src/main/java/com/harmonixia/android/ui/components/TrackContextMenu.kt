package com.harmonixia.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistRemove
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
    onAddToFavorites: () -> Unit,
    onRemoveFromFavorites: () -> Unit,
    isFavorite: Boolean,
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
            },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.track_action_add_to_playlist)) },
            onClick = {
                onAddToPlaylist()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.PlaylistAdd, contentDescription = null)
            }
        )
        if (!isFavorite) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.track_action_add_to_favorites)) },
                onClick = {
                    onAddToFavorites()
                    onDismissRequest()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Favorite, contentDescription = null)
                }
            )
        } else {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.track_action_remove_from_favorites)) },
                onClick = {
                    onRemoveFromFavorites()
                    onDismissRequest()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Favorite, contentDescription = null)
                }
            )
        }
        if (isEditable) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.track_action_remove_from_playlist)) },
                onClick = {
                    onRemoveFromPlaylist()
                    onDismissRequest()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.PlaylistRemove, contentDescription = null)
                }
            )
        }
    }
}
