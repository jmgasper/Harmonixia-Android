package com.harmonixia.android.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.ui.theme.HarmonixiaTheme

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreviewInitial() {
    HarmonixiaTheme {
        OnboardingScreenContent(
            uiState = OnboardingUiState.Initial(OnboardingFormState()),
            connectionState = ConnectionState.Disconnected,
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onConnect = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreviewConnecting() {
    HarmonixiaTheme {
        OnboardingScreenContent(
            uiState = OnboardingUiState.Connecting(
                OnboardingFormState(serverUrl = "http://192.168.1.29:8095")
            ),
            connectionState = ConnectionState.Connecting,
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onConnect = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreviewError() {
    HarmonixiaTheme {
        OnboardingScreenContent(
            uiState = OnboardingUiState.Error(
                OnboardingFormState(serverUrl = "http://192.168.1.29:8095"),
                message = "Connection failed"
            ),
            connectionState = ConnectionState.Error("Connection failed"),
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onConnect = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreviewSuccess() {
    HarmonixiaTheme {
        OnboardingScreenContent(
            uiState = OnboardingUiState.Success(
                OnboardingFormState(serverUrl = "http://192.168.1.29:8095")
            ),
            connectionState = ConnectionState.Connected,
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onConnect = {},
            onClearError = {}
        )
    }
}
