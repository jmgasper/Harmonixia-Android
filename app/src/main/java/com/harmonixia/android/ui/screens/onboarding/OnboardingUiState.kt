package com.harmonixia.android.ui.screens.onboarding

data class OnboardingFormState(
    val serverUrl: String = "",
    val authToken: String = "",
    val isServerUrlValid: Boolean = true,
    val isAuthTokenValid: Boolean = true,
    val serverUrlError: String? = null,
    val authTokenError: String? = null
) {
    val isFormValid: Boolean
        get() = isServerUrlValid && isAuthTokenValid && serverUrl.isNotBlank()
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
