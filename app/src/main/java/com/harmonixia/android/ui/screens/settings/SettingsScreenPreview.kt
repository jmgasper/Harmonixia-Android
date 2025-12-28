package com.harmonixia.android.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
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
                canDisconnect = false
            ),
            storedServerUrl = "http://192.168.1.29:8095",
            storedAuthToken = "",
            snackbarHostState = remember { SnackbarHostState() },
            eqEnabled = false,
            eqPresetName = null,
            onNavigateBack = {},
            onNavigateToEqSettings = {},
            onNavigateToPerformanceSettings = {},
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onTestConnection = {},
            onSaveConnection = {},
            onDisconnect = {},
            onClearSettings = {},
            onClearError = {}
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
                isTesting = true
            ),
            storedServerUrl = "http://192.168.1.29:8095",
            storedAuthToken = "token",
            snackbarHostState = remember { SnackbarHostState() },
            eqEnabled = true,
            eqPresetName = "Studio Monitor",
            onNavigateBack = {},
            onNavigateToEqSettings = {},
            onNavigateToPerformanceSettings = {},
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onTestConnection = {},
            onSaveConnection = {},
            onDisconnect = {},
            onClearSettings = {},
            onClearError = {}
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
                message = "Connection failed"
            ),
            storedServerUrl = "http://192.168.1.29:8095",
            storedAuthToken = "token",
            snackbarHostState = remember { SnackbarHostState() },
            eqEnabled = false,
            eqPresetName = "Reference",
            onNavigateBack = {},
            onNavigateToEqSettings = {},
            onNavigateToPerformanceSettings = {},
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onTestConnection = {},
            onSaveConnection = {},
            onDisconnect = {},
            onClearSettings = {},
            onClearError = {}
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
                message = "Connected successfully"
            ),
            storedServerUrl = "http://192.168.1.29:8095",
            storedAuthToken = "token",
            snackbarHostState = remember { SnackbarHostState() },
            eqEnabled = true,
            eqPresetName = "Reference",
            onNavigateBack = {},
            onNavigateToEqSettings = {},
            onNavigateToPerformanceSettings = {},
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onTestConnection = {},
            onSaveConnection = {},
            onDisconnect = {},
            onClearSettings = {},
            onClearError = {}
        )
    }
}
