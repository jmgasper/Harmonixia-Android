package com.harmonixia.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R
import com.harmonixia.android.data.remote.ConnectionState

@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
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
        is ConnectionState.Disconnected -> Color(0xFFC62828) to
            stringResource(R.string.status_disconnected)
    }
    val statusContentDescription = if (onClick != null) {
        "${stringResource(R.string.content_desc_connection_indicator)} $label"
    } else {
        "${stringResource(R.string.content_desc_connection_status)}: $label"
    }
    val interactionSource = remember { MutableInteractionSource() }
    val rippleIndication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            onClick = onClick,
            interactionSource = interactionSource,
            indication = rippleIndication
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .testTag("connection_status_indicator")
            .padding(8.dp)
            .then(clickableModifier)
            .semantics { contentDescription = statusContentDescription }
    ) {
        Row {
            Spacer(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
        }
    }
}
