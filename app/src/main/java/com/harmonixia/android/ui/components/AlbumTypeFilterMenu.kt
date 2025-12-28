package com.harmonixia.android.ui.components

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.AlbumType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumTypeFilterMenu(
    selectedTypes: Set<AlbumType>,
    onTypeToggle: (AlbumType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val albumTypes = remember {
        listOf(
            AlbumType.ALBUM,
            AlbumType.SINGLE,
            AlbumType.EP,
            AlbumType.COMPILATION
        )
    }

    val selectAll = {
        albumTypes.filterNot { selectedTypes.contains(it) }.forEach(onTypeToggle)
    }
    val clearAll = {
        albumTypes.filter { selectedTypes.contains(it) }.forEach(onTypeToggle)
    }

    if (isCompact) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = selectAll) {
                        Text(text = stringResource(R.string.filter_select_all))
                    }
                    TextButton(onClick = clearAll) {
                        Text(text = stringResource(R.string.filter_clear_all))
                    }
                }
                albumTypes.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTypeToggle(type) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTypes.contains(type),
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = albumTypeLabel(type))
                    }
                }
            }
        }
    } else {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            modifier = modifier
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.filter_select_all)) },
                onClick = selectAll
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.filter_clear_all)) },
                onClick = clearAll
            )
            HorizontalDivider()
            albumTypes.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedTypes.contains(type),
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = albumTypeLabel(type))
                        }
                    },
                    onClick = { onTypeToggle(type) }
                )
            }
        }
    }
}

@Composable
private fun albumTypeLabel(type: AlbumType): String {
    return when (type) {
        AlbumType.ALBUM -> stringResource(R.string.album_type_album)
        AlbumType.SINGLE -> stringResource(R.string.album_type_single)
        AlbumType.EP -> stringResource(R.string.album_type_ep)
        AlbumType.COMPILATION -> stringResource(R.string.album_type_compilation)
        AlbumType.UNKNOWN -> type.name
    }
}
