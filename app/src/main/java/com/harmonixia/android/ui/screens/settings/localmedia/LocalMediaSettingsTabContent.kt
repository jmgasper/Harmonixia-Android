package com.harmonixia.android.ui.screens.settings.localmedia

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.harmonixia.android.R
import com.harmonixia.android.data.local.LocalMediaScanner
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.LoadingButton

@Composable
fun LocalMediaSettingsTabContent(
    uiState: LocalMediaSettingsUiState,
    onSelectFolder: () -> Unit,
    onScanLocalMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val localMediaFolderContentDescription = stringResource(R.string.content_desc_local_media_folder)
    val displayFolderName = remember(uiState.folderUri, context) {
        resolveLocalMediaFolderName(uiState.folderUri, context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.section_local_media),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.local_media_folder_label),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = displayFolderName.ifBlank {
                stringResource(R.string.local_media_no_folder_selected)
            },
            onValueChange = {},
            readOnly = true,
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_local_media_folder")
                .semantics {
                    contentDescription = localMediaFolderContentDescription
                }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.local_media_available_files, uiState.trackCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSelectFolder,
                enabled = !uiState.scanState.isScanning,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.local_media_select_folder))
            }

            LoadingButton(
                text = stringResource(R.string.local_media_refresh),
                onClick = onScanLocalMedia,
                enabled = uiState.folderUri.isNotBlank() && !uiState.scanState.isScanning,
                isLoading = uiState.scanState.isScanning,
                modifier = Modifier.weight(1f),
                testTag = "settings_scan_local_media"
            )
        }

        when (val progress = uiState.scanState.progress) {
            is LocalMediaScanner.ScanProgress.Scanning -> {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.current.toFloat() / progress.total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.local_media_scanning_files,
                        progress.total,
                        progress.current,
                        progress.total
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is LocalMediaScanner.ScanProgress.Complete -> {
                Spacer(modifier = Modifier.height(12.dp))
                LocalMediaStatusCard(
                    message = stringResource(
                        R.string.local_media_scan_complete,
                        progress.result.tracksAdded,
                        progress.result.albumsAdded,
                        progress.result.artistsAdded
                    )
                )
            }
            is LocalMediaScanner.ScanProgress.Error -> {
                Spacer(modifier = Modifier.height(12.dp))
                ErrorCard(
                    message = stringResource(
                        R.string.local_media_scan_error,
                        progress.message
                    ),
                    onDismiss = { /* Clear error state if needed */ },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LocalMediaScanner.ScanProgress.Idle -> {
                // No status to show.
            }
        }
    }
}

private fun resolveLocalMediaFolderName(
    folderUri: String,
    context: Context
): String {
    if (folderUri.isBlank()) {
        return ""
    }
    val uri = runCatching { folderUri.toUri() }.getOrNull() ?: return folderUri
    val documentName = DocumentFile.fromTreeUri(context, uri)?.name
    if (!documentName.isNullOrBlank()) {
        return documentName
    }
    val treeId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
    val rawName = treeId ?: uri.lastPathSegment ?: folderUri
    val decoded = Uri.decode(rawName)
    val afterColon = decoded.substringAfterLast(':', decoded)
    val leafName = afterColon.substringAfterLast('/', afterColon)
    return leafName.ifBlank { decoded.ifBlank { folderUri } }
}

@Composable
private fun LocalMediaStatusCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_success"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = stringResource(R.string.content_desc_success_icon),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
