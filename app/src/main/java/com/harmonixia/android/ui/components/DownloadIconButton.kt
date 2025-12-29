package com.harmonixia.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.DownloadProgress

@Composable
fun DownloadIconButton(
    onClick: () -> Unit,
    overallProgress: DownloadProgress,
    modifier: Modifier = Modifier
) {
    val showProgress = overallProgress.totalBytes > 0L || overallProgress.bytesDownloaded > 0L
    val progressValue = overallProgress.progress.coerceIn(0, 100)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = stringResource(R.string.content_desc_download_icon)
            )
        }
        if (showProgress) {
            DownloadProgressIndicator(
                progress = progressValue,
                size = 28.dp,
                showLabel = false,
                strokeWidth = 2.dp
            )
        }
    }
}
