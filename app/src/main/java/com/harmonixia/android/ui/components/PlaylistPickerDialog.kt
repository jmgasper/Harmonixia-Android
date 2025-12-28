package com.harmonixia.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Playlist

@Composable
fun PlaylistPickerDialog(
    playlists: List<Playlist>,
    onPlaylistSelected: (Playlist) -> Unit,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(playlists, query) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            playlists
        } else {
            playlists.filter { playlist ->
                playlist.name.contains(trimmed, ignoreCase = true) ||
                    playlist.owner?.contains(trimmed, ignoreCase = true) == true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.playlist_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(text = stringResource(R.string.playlist_picker_search_hint))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    item {
                        TextButton(
                            onClick = onCreateNew,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.playlist_picker_create_new))
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.playlist_picker_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(filtered, key = { playlist -> "${playlist.provider}:${playlist.itemId}" }) { playlist ->
                            val isEditable = playlist.isEditable
                            val owner = playlist.owner?.trim().orEmpty()
                            val supporting = when {
                                !isEditable && owner.isNotBlank() ->
                                    stringResource(R.string.playlist_detail_owner, owner)
                                !isEditable -> stringResource(R.string.playlist_picker_read_only)
                                else -> null
                            }
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = playlist.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isEditable) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                },
                                supportingContent = supporting?.let {
                                    { Text(text = it) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isEditable) {
                                        onPlaylistSelected(playlist)
                                    }
                            )
                        }
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
