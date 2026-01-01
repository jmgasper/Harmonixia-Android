package com.harmonixia.android.ui.screens.settings

import com.harmonixia.android.data.local.LocalMediaScanner
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.AuthMethod

data class SettingsFormState(
    val serverUrl: String = "",
    val authToken: String = "",
    val authMethod: AuthMethod = AuthMethod.TOKEN,
    val username: String = "",
    val password: String = "",
    val isServerUrlValid: Boolean = true,
    val isAuthTokenValid: Boolean = true,
    val isUsernameValid: Boolean = true,
    val isPasswordValid: Boolean = true,
    val serverUrlError: String? = null,
    val authTokenError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isModified: Boolean = false
) {
    val isFormValid: Boolean
        get() = when (authMethod) {
            AuthMethod.TOKEN -> serverUrl.isNotBlank() &&
                isServerUrlValid &&
                authToken.isNotBlank() &&
                isAuthTokenValid
            AuthMethod.USERNAME_PASSWORD -> serverUrl.isNotBlank() &&
                isServerUrlValid &&
                username.isNotBlank() &&
                password.isNotBlank() &&
                isUsernameValid &&
                isPasswordValid
        }
}

data class LocalMediaScanState(
    val isScanning: Boolean = false,
    val progress: LocalMediaScanner.ScanProgress = LocalMediaScanner.ScanProgress.Idle
)

enum class SettingsTab {
    CONNECTION,
    EQUALIZER,
    LOCAL_MEDIA
}

sealed class SettingsUiState(
    open val form: SettingsFormState,
    open val connectionState: ConnectionState,
    open val canDisconnect: Boolean,
    open val localMediaScanState: LocalMediaScanState,
    open val selectedTab: SettingsTab
) {
    data class Initial(
        override val form: SettingsFormState,
        override val connectionState: ConnectionState,
        override val canDisconnect: Boolean,
        override val localMediaScanState: LocalMediaScanState = LocalMediaScanState(),
        override val selectedTab: SettingsTab
    ) : SettingsUiState(form, connectionState, canDisconnect, localMediaScanState, selectedTab)

    data class Validating(
        override val form: SettingsFormState,
        override val connectionState: ConnectionState,
        override val canDisconnect: Boolean,
        override val localMediaScanState: LocalMediaScanState = LocalMediaScanState(),
        override val selectedTab: SettingsTab
    ) : SettingsUiState(form, connectionState, canDisconnect, localMediaScanState, selectedTab)

    data class Connecting(
        override val form: SettingsFormState,
        override val connectionState: ConnectionState,
        override val canDisconnect: Boolean,
        override val localMediaScanState: LocalMediaScanState = LocalMediaScanState(),
        override val selectedTab: SettingsTab,
        val isTesting: Boolean
    ) : SettingsUiState(form, connectionState, canDisconnect, localMediaScanState, selectedTab)

    data class Success(
        override val form: SettingsFormState,
        override val connectionState: ConnectionState,
        override val canDisconnect: Boolean,
        override val localMediaScanState: LocalMediaScanState = LocalMediaScanState(),
        override val selectedTab: SettingsTab,
        val message: String
    ) : SettingsUiState(form, connectionState, canDisconnect, localMediaScanState, selectedTab)

    data class Error(
        override val form: SettingsFormState,
        override val connectionState: ConnectionState,
        override val canDisconnect: Boolean,
        override val localMediaScanState: LocalMediaScanState = LocalMediaScanState(),
        override val selectedTab: SettingsTab,
        val message: String
    ) : SettingsUiState(form, connectionState, canDisconnect, localMediaScanState, selectedTab)
}

fun SettingsUiState.withConnectionState(
    state: ConnectionState,
    canDisconnect: Boolean
): SettingsUiState {
    return when (this) {
        is SettingsUiState.Initial -> copy(
            connectionState = state,
            canDisconnect = canDisconnect,
            localMediaScanState = localMediaScanState
        )
        is SettingsUiState.Validating -> copy(
            connectionState = state,
            canDisconnect = canDisconnect,
            localMediaScanState = localMediaScanState
        )
        is SettingsUiState.Connecting -> copy(
            connectionState = state,
            canDisconnect = canDisconnect,
            localMediaScanState = localMediaScanState
        )
        is SettingsUiState.Success -> copy(
            connectionState = state,
            canDisconnect = canDisconnect,
            localMediaScanState = localMediaScanState
        )
        is SettingsUiState.Error -> copy(
            connectionState = state,
            canDisconnect = canDisconnect,
            localMediaScanState = localMediaScanState
        )
    }
}

fun SettingsUiState.withLocalMediaScanState(
    scanState: LocalMediaScanState
): SettingsUiState {
    return when (this) {
        is SettingsUiState.Initial -> copy(localMediaScanState = scanState)
        is SettingsUiState.Validating -> copy(localMediaScanState = scanState)
        is SettingsUiState.Connecting -> copy(localMediaScanState = scanState)
        is SettingsUiState.Success -> copy(localMediaScanState = scanState)
        is SettingsUiState.Error -> copy(localMediaScanState = scanState)
    }
}

fun SettingsUiState.withSelectedTab(
    tab: SettingsTab
): SettingsUiState {
    return when (this) {
        is SettingsUiState.Initial -> copy(selectedTab = tab)
        is SettingsUiState.Validating -> copy(selectedTab = tab)
        is SettingsUiState.Connecting -> copy(selectedTab = tab)
        is SettingsUiState.Success -> copy(selectedTab = tab)
        is SettingsUiState.Error -> copy(selectedTab = tab)
    }
}
