package com.harmonixia.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R

@Composable
fun DownloadProgressIndicator(
    progress: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 3.dp,
    showLabel: Boolean = true
) {
    val clampedProgress = progress.coerceIn(0, 100)
    val description = stringResource(R.string.content_desc_download_progress)
    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { clampedProgress / 100f },
            strokeWidth = strokeWidth,
            modifier = Modifier.fillMaxSize()
        )
        if (showLabel) {
            Text(
                text = "${clampedProgress}%",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
