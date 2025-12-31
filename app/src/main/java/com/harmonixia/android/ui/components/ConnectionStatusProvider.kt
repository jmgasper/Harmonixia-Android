package com.harmonixia.android.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.ui.navigation.LocalMainScaffoldActions
import com.harmonixia.android.ui.screens.settings.SettingsTab
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ConnectionStatusProvider(
    connectionState: StateFlow<ConnectionState>,
    onNavigateToSettings: (SettingsTab?) -> Unit,
    content: @Composable () -> Unit
) {
    val collectedState = connectionState.collectAsStateWithLifecycle()
    ConnectionStatusProvider(
        connectionState = collectedState,
        onNavigateToSettings = onNavigateToSettings,
        content = content
    )
}

@Composable
fun ConnectionStatusProvider(
    connectionState: State<ConnectionState>,
    onNavigateToSettings: (SettingsTab?) -> Unit,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalMainScaffoldActions provides {
            ConnectionStatusIndicator(
                connectionState = connectionState.value,
                onClick = { onNavigateToSettings(SettingsTab.CONNECTION) }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    ) {
        content()
    }
}
