package com.harmonixia.android.ui.screens.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.harmonixia.android.R
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.ui.screens.PREVIEW_DEMO_SERVER_URL
import com.harmonixia.android.ui.screens.settings.localmedia.LocalMediaSettingsUiState
import com.harmonixia.android.ui.theme.HarmonixiaTheme

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreviewInitial() {
    HarmonixiaTheme {
        SettingsScreenContent(
            uiState = SettingsUiState.Initial(
                form = SettingsFormState(serverUrl = PREVIEW_DEMO_SERVER_URL),
                connectionState = ConnectionState.Disconnected,
                canDisconnect = false,
                selectedTab = SettingsTab.CONNECTION
            ),
            selectedTab = SettingsTab.CONNECTION,
            onTabSelected = {},
            snackbarHostState = remember { SnackbarHostState() },
            localMediaUiState = LocalMediaSettingsUiState(),
            onNavigateBack = {},
            onNavigateToPerformanceSettings = {},
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onAuthMethodChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onTestConnection = {},
            onSaveConnection = {},
            onDisconnect = {},
            onClearSettings = {},
            onClearError = {},
            onSelectFolder = {},
            onScanLocalMedia = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreviewConnecting() {
    HarmonixiaTheme {
        SettingsScreenContent(
            uiState = SettingsUiState.Connecting(
                form = SettingsFormState(serverUrl = PREVIEW_DEMO_SERVER_URL),
                connectionState = ConnectionState.Connecting,
                canDisconnect = true,
                selectedTab = SettingsTab.CONNECTION,
                isTesting = true
            ),
            selectedTab = SettingsTab.CONNECTION,
            onTabSelected = {},
            snackbarHostState = remember { SnackbarHostState() },
            localMediaUiState = LocalMediaSettingsUiState(),
            onNavigateBack = {},
            onNavigateToPerformanceSettings = {},
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onAuthMethodChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onTestConnection = {},
            onSaveConnection = {},
            onDisconnect = {},
            onClearSettings = {},
            onClearError = {},
            onSelectFolder = {},
            onScanLocalMedia = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreviewError() {
    HarmonixiaTheme {
        val connectionFailed = stringResource(R.string.status_connection_failed)
        SettingsScreenContent(
            uiState = SettingsUiState.Error(
                form = SettingsFormState(serverUrl = PREVIEW_DEMO_SERVER_URL),
                connectionState = ConnectionState.Error(connectionFailed),
                canDisconnect = false,
                selectedTab = SettingsTab.CONNECTION,
                message = connectionFailed
            ),
            selectedTab = SettingsTab.CONNECTION,
            onTabSelected = {},
            snackbarHostState = remember { SnackbarHostState() },
            localMediaUiState = LocalMediaSettingsUiState(),
            onNavigateBack = {},
            onNavigateToPerformanceSettings = {},
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onAuthMethodChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onTestConnection = {},
            onSaveConnection = {},
            onDisconnect = {},
            onClearSettings = {},
            onClearError = {},
            onSelectFolder = {},
            onScanLocalMedia = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreviewSuccess() {
    HarmonixiaTheme {
        val connected = stringResource(R.string.status_connected)
        SettingsScreenContent(
            uiState = SettingsUiState.Success(
                form = SettingsFormState(serverUrl = PREVIEW_DEMO_SERVER_URL),
                connectionState = ConnectionState.Connected,
                canDisconnect = true,
                selectedTab = SettingsTab.CONNECTION,
                message = connected
            ),
            selectedTab = SettingsTab.CONNECTION,
            onTabSelected = {},
            snackbarHostState = remember { SnackbarHostState() },
            localMediaUiState = LocalMediaSettingsUiState(),
            onNavigateBack = {},
            onNavigateToPerformanceSettings = {},
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onAuthMethodChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onTestConnection = {},
            onSaveConnection = {},
            onDisconnect = {},
            onClearSettings = {},
            onClearError = {},
            onSelectFolder = {},
            onScanLocalMedia = {}
        )
    }
}
