package com.harmonixia.android.ui.screens.settings

import com.harmonixia.android.data.remote.ConnectionState

data class SettingsFormState(
    val serverUrl: String = "",
    val authToken: String = "",
    val isServerUrlValid: Boolean = true,
    val isAuthTokenValid: Boolean = true,
    val serverUrlError: String? = null,
    val authTokenError: String? = null,
    val isModified: Boolean = false
) {
    val isFormValid: Boolean
        get() = isServerUrlValid && isAuthTokenValid && serverUrl.isNotBlank()
}

sealed class SettingsUiState(
    open val form: SettingsFormState,
    open val connectionState: ConnectionState,
    open val canDisconnect: Boolean
) {
    data class Initial(
        override val form: SettingsFormState,
        override val connectionState: ConnectionState,
        override val canDisconnect: Boolean
    ) : SettingsUiState(form, connectionState, canDisconnect)

    data class Validating(
        override val form: SettingsFormState,
        override val connectionState: ConnectionState,
        override val canDisconnect: Boolean
    ) : SettingsUiState(form, connectionState, canDisconnect)

    data class Connecting(
        override val form: SettingsFormState,
        override val connectionState: ConnectionState,
        override val canDisconnect: Boolean,
        val isTesting: Boolean
    ) : SettingsUiState(form, connectionState, canDisconnect)

    data class Success(
        override val form: SettingsFormState,
        override val connectionState: ConnectionState,
        override val canDisconnect: Boolean,
        val message: String
    ) : SettingsUiState(form, connectionState, canDisconnect)

    data class Error(
        override val form: SettingsFormState,
        override val connectionState: ConnectionState,
        override val canDisconnect: Boolean,
        val message: String
    ) : SettingsUiState(form, connectionState, canDisconnect)
}
