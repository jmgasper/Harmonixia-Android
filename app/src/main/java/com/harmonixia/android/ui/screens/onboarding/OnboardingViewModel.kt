package com.harmonixia.android.ui.screens.onboarding

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.AuthMethod
import com.harmonixia.android.domain.usecase.ConnectToServerUseCase
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.NetworkError
import com.harmonixia.android.util.UrlValidationError
import com.harmonixia.android.util.ValidationUtils
import com.harmonixia.android.util.toNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val getConnectionStateUseCase: GetConnectionStateUseCase,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow<OnboardingUiState>(
        OnboardingUiState.Initial(loadFormState())
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = getConnectionStateUseCase()

    private var connectJob: Job? = null
    private var lastConnectTimestamp: Long = 0L

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.getServerUrl(),
                settingsDataStore.getAuthToken()
            ) { url, token -> url to token }
                .collect { (url, token) ->
                    val current = _uiState.value.form
                    if (current.serverUrl.isBlank() && url.isNotBlank()) {
                        updateForm {
                            it.copy(serverUrl = url, authToken = token)
                        }
                    }
                }
        }

        viewModelScope.launch {
            connectionState.collect { state ->
                if (state is ConnectionState.Error) {
                    val message = mapErrorMessage(IllegalStateException(state.message))
                    updateStateForError(message)
                }
            }
        }
    }

    fun updateServerUrl(value: String) {
        savedStateHandle[KEY_SERVER_URL] = value
        val validation = ValidationUtils.validateServerUrl(value)
        val errorMessage = validation.error?.let { errorMessageFor(it) }
        updateForm {
            it.copy(
                serverUrl = value,
                isServerUrlValid = validation.isValid,
                serverUrlError = errorMessage
            )
        }
    }

    fun updateAuthToken(value: String) {
        savedStateHandle[KEY_AUTH_TOKEN] = value
        updateForm {
            it.copy(
                authToken = value,
                isAuthTokenValid = true,
                authTokenError = null
            )
        }
    }

    fun updateAuthMethod(value: AuthMethod) {
        savedStateHandle[KEY_AUTH_METHOD] = value
        updateForm { current ->
            when (value) {
                AuthMethod.TOKEN -> {
                    savedStateHandle[KEY_USERNAME] = ""
                    savedStateHandle[KEY_PASSWORD] = ""
                    current.copy(
                        authMethod = value,
                        username = "",
                        password = "",
                        isUsernameValid = true,
                        isPasswordValid = true,
                        usernameError = null,
                        passwordError = null
                    )
                }
                AuthMethod.USERNAME_PASSWORD -> {
                    savedStateHandle[KEY_AUTH_TOKEN] = ""
                    current.copy(
                        authMethod = value,
                        authToken = "",
                        isAuthTokenValid = true,
                        authTokenError = null
                    )
                }
            }
        }
    }

    fun updateUsername(value: String) {
        savedStateHandle[KEY_USERNAME] = value
        val isValid = value.isNotBlank()
        val errorMessage = if (isValid) null else {
            context.getString(R.string.error_username_required)
        }
        updateForm {
            it.copy(
                username = value,
                isUsernameValid = isValid,
                usernameError = errorMessage
            )
        }
    }

    fun updatePassword(value: String) {
        savedStateHandle[KEY_PASSWORD] = value
        val isValid = value.isNotBlank()
        val errorMessage = if (isValid) null else {
            context.getString(R.string.error_password_required)
        }
        updateForm {
            it.copy(
                password = value,
                isPasswordValid = isValid,
                passwordError = errorMessage
            )
        }
    }

    fun clearError() {
        val current = _uiState.value
        if (current is OnboardingUiState.Error) {
            _uiState.value = OnboardingUiState.Initial(current.form)
        }
    }

    fun connect() {
        if (!canStartConnection()) return
        if (!validateInputs()) return
        val form = _uiState.value.form
        _uiState.value = OnboardingUiState.Connecting(form)
        connectJob = viewModelScope.launch {
            val normalizedUrl = ValidationUtils.normalizeUrl(form.serverUrl)
            val token = form.authToken.trim()
            Logger.i(TAG, "Attempting connection to ${Logger.sanitizeUrl(normalizedUrl)}")
            val result = connectToServerUseCase(
                normalizedUrl,
                token,
                form.authMethod,
                form.username.trim(),
                form.password.trim()
            )
            result.onSuccess {
                _uiState.value = OnboardingUiState.Success(form)
                clearSensitiveData()
            }.onFailure { error ->
                val message = mapErrorMessage(error)
                updateStateForError(message)
            }
        }
    }

    private fun validateInputs(): Boolean {
        _uiState.value = OnboardingUiState.Validating(_uiState.value.form)
        val form = _uiState.value.form
        val validation = ValidationUtils.validateServerUrl(form.serverUrl)
        val errorMessage = validation.error?.let { errorMessageFor(it) }
        if (!validation.isValid) {
            Logger.w(TAG, "Validation failed: ${validation.error}")
        }
        val isUsernameValid = if (form.authMethod == AuthMethod.USERNAME_PASSWORD) {
            form.username.isNotBlank()
        } else {
            form.isUsernameValid
        }
        val isPasswordValid = if (form.authMethod == AuthMethod.USERNAME_PASSWORD) {
            form.password.isNotBlank()
        } else {
            form.isPasswordValid
        }
        val usernameError = if (form.authMethod == AuthMethod.USERNAME_PASSWORD && !isUsernameValid) {
            context.getString(R.string.error_username_required)
        } else {
            form.usernameError
        }
        val passwordError = if (form.authMethod == AuthMethod.USERNAME_PASSWORD && !isPasswordValid) {
            context.getString(R.string.error_password_required)
        } else {
            form.passwordError
        }
        updateForm {
            it.copy(
                isServerUrlValid = validation.isValid,
                serverUrlError = errorMessage,
                isUsernameValid = isUsernameValid,
                isPasswordValid = isPasswordValid,
                usernameError = usernameError,
                passwordError = passwordError
            )
        }
        val isValid = validation.isValid && when (form.authMethod) {
            AuthMethod.TOKEN -> true
            AuthMethod.USERNAME_PASSWORD -> isUsernameValid && isPasswordValid
        }
        if (!isValid) {
            _uiState.value = OnboardingUiState.Initial(_uiState.value.form)
        }
        return isValid
    }

    private fun updateForm(transform: (OnboardingFormState) -> OnboardingFormState) {
        _uiState.update { current ->
            val updatedForm = transform(current.form)
            when (current) {
                is OnboardingUiState.Initial -> current.copy(form = updatedForm)
                is OnboardingUiState.Validating -> current.copy(form = updatedForm)
                is OnboardingUiState.Connecting -> current.copy(form = updatedForm)
                is OnboardingUiState.Success -> current.copy(form = updatedForm)
                is OnboardingUiState.Error -> current.copy(form = updatedForm)
            }
        }
    }

    private fun updateStateForError(message: String) {
        _uiState.update { current ->
            if (current is OnboardingUiState.Error) {
                current.copy(message = message)
            } else {
                OnboardingUiState.Error(current.form, message)
            }
        }
    }

    private fun errorMessageFor(error: UrlValidationError): String {
        return when (error) {
            UrlValidationError.EMPTY -> context.getString(R.string.error_server_url_required)
            UrlValidationError.INVALID_FORMAT -> context.getString(R.string.error_invalid_url)
        }
    }

    private fun mapErrorMessage(throwable: Throwable): String {
        val lowered = throwable.message?.lowercase().orEmpty()
        val networkError = throwable.toNetworkError()
        return when {
            "authentication" in lowered || "unauthorized" in lowered || "forbidden" in lowered ->
                context.getString(R.string.status_auth_failed)
            networkError is NetworkError.AuthenticationError ->
                context.getString(R.string.status_auth_failed)
            throwable is TimeoutCancellationException || "timeout" in lowered ->
                context.getString(R.string.error_connection_timeout)
            networkError is NetworkError.TimeoutError ->
                context.getString(R.string.error_connection_timeout)
            else -> context.getString(R.string.status_connection_failed)
        }
    }

    private fun canStartConnection(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (connectJob?.isActive == true) return false
        if (now - lastConnectTimestamp < CONNECT_DEBOUNCE_MS) return false
        lastConnectTimestamp = now
        return true
    }

    private fun clearSensitiveData() {
        updateForm { it.copy(authToken = "", username = "", password = "") }
        savedStateHandle[KEY_AUTH_TOKEN] = ""
        savedStateHandle[KEY_USERNAME] = ""
        savedStateHandle[KEY_PASSWORD] = ""
    }

    private fun loadFormState(): OnboardingFormState {
        val serverUrl = savedStateHandle.get<String>(KEY_SERVER_URL).orEmpty()
        val authToken = savedStateHandle.get<String>(KEY_AUTH_TOKEN).orEmpty()
        val authMethod = savedStateHandle.get<AuthMethod>(KEY_AUTH_METHOD)
            ?: savedStateHandle.get<String>(KEY_AUTH_METHOD)?.let { stored ->
                runCatching { AuthMethod.valueOf(stored) }.getOrNull()
            }
            ?: AuthMethod.TOKEN
        val username = savedStateHandle.get<String>(KEY_USERNAME).orEmpty()
        val password = savedStateHandle.get<String>(KEY_PASSWORD).orEmpty()
        return OnboardingFormState(
            serverUrl = serverUrl,
            authToken = authToken,
            authMethod = authMethod,
            username = username,
            password = password
        )
    }

    private companion object {
        private const val TAG = "OnboardingViewModel"
        private const val CONNECT_DEBOUNCE_MS = 1_000L
        private const val KEY_SERVER_URL = "onboarding_server_url"
        private const val KEY_AUTH_TOKEN = "onboarding_auth_token"
        private const val KEY_AUTH_METHOD = "onboarding_auth_method"
        private const val KEY_USERNAME = "onboarding_username"
        private const val KEY_PASSWORD = "onboarding_password"
    }
}
