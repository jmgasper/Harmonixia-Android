package com.harmonixia.android.ui.screens.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.ui.theme.HarmonixiaTheme

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreviewInitial() {
    HarmonixiaTheme {
        SettingsScreenContent(
            uiState = SettingsUiState.Initial(
                form = SettingsFormState(serverUrl = "http://192.168.1.29:8095"),
                connectionState = ConnectionState.Disconnected,
                canDisconnect = false,
                selectedTab = SettingsTab.CONNECTION
            ),
            selectedTab = SettingsTab.CONNECTION,
            onTabSelected = {},
            snackbarHostState = remember { SnackbarHostState() },
            localMediaFolderUri = "",
            localMediaTrackCount = 0,
            localMediaScanState = LocalMediaScanState(),
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
                form = SettingsFormState(serverUrl = "http://192.168.1.29:8095"),
                connectionState = ConnectionState.Connecting,
                canDisconnect = true,
                selectedTab = SettingsTab.CONNECTION,
                isTesting = true
            ),
            selectedTab = SettingsTab.CONNECTION,
            onTabSelected = {},
            snackbarHostState = remember { SnackbarHostState() },
            localMediaFolderUri = "",
            localMediaTrackCount = 0,
            localMediaScanState = LocalMediaScanState(),
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
        SettingsScreenContent(
            uiState = SettingsUiState.Error(
                form = SettingsFormState(serverUrl = "http://192.168.1.29:8095"),
                connectionState = ConnectionState.Error("Connection failed"),
                canDisconnect = false,
                selectedTab = SettingsTab.CONNECTION,
                message = "Connection failed"
            ),
            selectedTab = SettingsTab.CONNECTION,
            onTabSelected = {},
            snackbarHostState = remember { SnackbarHostState() },
            localMediaFolderUri = "",
            localMediaTrackCount = 0,
            localMediaScanState = LocalMediaScanState(),
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
        SettingsScreenContent(
            uiState = SettingsUiState.Success(
                form = SettingsFormState(serverUrl = "http://192.168.1.29:8095"),
                connectionState = ConnectionState.Connected,
                canDisconnect = true,
                selectedTab = SettingsTab.CONNECTION,
                message = "Connected successfully"
            ),
            selectedTab = SettingsTab.CONNECTION,
            onTabSelected = {},
            snackbarHostState = remember { SnackbarHostState() },
            localMediaFolderUri = "",
            localMediaTrackCount = 0,
            localMediaScanState = LocalMediaScanState(),
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
