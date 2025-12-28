package com.harmonixia.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R
import com.harmonixia.android.data.remote.ConnectionState
import androidx.compose.ui.graphics.Color

@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (connectionState) {
        is ConnectionState.Connected -> Color(0xFF2E7D32) to stringResource(R.string.status_connected)
        is ConnectionState.Connecting -> Color(0xFFF9A825) to stringResource(R.string.status_connecting)
        is ConnectionState.Error -> {
            val lowered = connectionState.message.lowercase()
            if ("auth" in lowered || "unauthorized" in lowered || "forbidden" in lowered) {
                Color(0xFFC62828) to stringResource(R.string.status_auth_failed)
            } else {
                Color(0xFFC62828) to stringResource(R.string.status_connection_failed)
            }
        }
        is ConnectionState.Disconnected -> MaterialTheme.colorScheme.outline to
            stringResource(R.string.status_disconnected)
    }
    val statusContentDescription =
        "${stringResource(R.string.content_desc_connection_status)}: $label"

    Row(
        modifier = modifier
            .testTag("connection_status_indicator")
            .semantics { contentDescription = statusContentDescription }
    ) {
        Spacer(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
