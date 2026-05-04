package com.harmonixia.android.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.harmonixia.android.R
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
            onAuthMethodChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
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
            onAuthMethodChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onConnect = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreviewError() {
    HarmonixiaTheme {
        val connectionFailed = stringResource(R.string.status_connection_failed)
        OnboardingScreenContent(
            uiState = OnboardingUiState.Error(
                OnboardingFormState(serverUrl = "http://192.168.1.29:8095"),
                message = connectionFailed
            ),
            connectionState = ConnectionState.Error(connectionFailed),
            onServerUrlChange = {},
            onAuthTokenChange = {},
            onAuthMethodChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
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
            onAuthMethodChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onConnect = {},
            onClearError = {}
        )
    }
}
