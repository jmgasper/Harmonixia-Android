package com.harmonixia.android.ui.screens.onboarding

import com.harmonixia.android.domain.model.AuthMethod

data class OnboardingFormState(
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
    val passwordError: String? = null
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

sealed class OnboardingUiState(open val form: OnboardingFormState) {
    data class Initial(override val form: OnboardingFormState) : OnboardingUiState(form)
    data class Validating(override val form: OnboardingFormState) : OnboardingUiState(form)
    data class Connecting(override val form: OnboardingFormState) : OnboardingUiState(form)
    data class Success(override val form: OnboardingFormState) : OnboardingUiState(form)
    data class Error(
        override val form: OnboardingFormState,
        val message: String
    ) : OnboardingUiState(form)
}
