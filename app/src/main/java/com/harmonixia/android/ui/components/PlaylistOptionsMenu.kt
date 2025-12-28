package com.harmonixia.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.harmonixia.android.R

@Composable
fun PlaylistOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.playlist_action_rename)) },
            onClick = {
                onRename()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.playlist_action_delete)) },
            onClick = {
                onDelete()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null
                )
            }
        )
    }
}
