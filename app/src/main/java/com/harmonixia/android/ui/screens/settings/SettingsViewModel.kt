package com.harmonixia.android.ui.screens.settings

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.EqSettings
import com.harmonixia.android.domain.repository.EqPresetRepository
import com.harmonixia.android.domain.usecase.ConnectToServerUseCase
import com.harmonixia.android.domain.usecase.DisconnectFromServerUseCase
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.UrlValidationError
import com.harmonixia.android.util.ValidationUtils
import com.harmonixia.android.util.toNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException

sealed class SettingsUiEvent {
    data class ShowSnackbar(val message: String) : SettingsUiEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val disconnectFromServerUseCase: DisconnectFromServerUseCase,
    private val getConnectionStateUseCase: GetConnectionStateUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val eqDataStore: EqDataStore,
    private val eqPresetRepository: EqPresetRepository,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val connectionState: StateFlow<ConnectionState> = getConnectionStateUseCase()

    private val _uiState = MutableStateFlow<SettingsUiState>(
        SettingsUiState.Initial(
            form = loadFormState(),
            connectionState = connectionState.value,
            canDisconnect = false
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _storedServerUrl = MutableStateFlow("")
    val storedServerUrl: StateFlow<String> = _storedServerUrl.asStateFlow()

    private val _storedAuthToken = MutableStateFlow("")
    val storedAuthToken: StateFlow<String> = _storedAuthToken.asStateFlow()

    private val _events = MutableSharedFlow<SettingsUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val _eqSettings = MutableStateFlow(EqSettings())
    val eqSettings: StateFlow<EqSettings> = _eqSettings.asStateFlow()

    private val _eqPresetName = MutableStateFlow<String?>(null)
    val eqPresetName: StateFlow<String?> = _eqPresetName.asStateFlow()

    private var originalServerUrl: String = ""
    private var originalAuthToken: String = ""
    private var operationJob: Job? = null
    private var lastOperationTimestamp: Long = 0L

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.getServerUrl(),
                settingsDataStore.getAuthToken()
            ) { url, token -> url to token }
                .collect { (url, token) ->
                    originalServerUrl = url
                    originalAuthToken = token
                    _storedServerUrl.value = url
                    _storedAuthToken.value = token
                    val currentForm = _uiState.value.form
                    val shouldReplace = !currentForm.isModified
                    if (shouldReplace) {
                        setFormState(
                            currentForm.copy(
                                serverUrl = url,
                                authToken = token,
                                isModified = false
                            )
                        )
                    }
                }
        }

        viewModelScope.launch {
            eqPresetRepository.loadPresets(forceRefresh = false)
        }

        viewModelScope.launch {
            eqDataStore.getEqSettings().collect { settings ->
                _eqSettings.value = settings
                val presetId = settings.selectedPresetId
                if (presetId.isNullOrBlank()) {
                    _eqPresetName.value = null
                    return@collect
                }
                if (eqPresetRepository.getPresetById(presetId) == null) {
                    eqPresetRepository.loadPresets(forceRefresh = false)
                }
                _eqPresetName.value = eqPresetRepository.getPresetById(presetId)?.displayName
            }
        }

        viewModelScope.launch {
            connectionState.collect { state ->
                val canDisconnect = state !is ConnectionState.Disconnected
                _uiState.update { current ->
                    current.withConnectionState(state, canDisconnect)
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
                serverUrlError = errorMessage,
                isModified = isModified(value, it.authToken)
            )
        }
    }

    fun updateAuthToken(value: String) {
        savedStateHandle[KEY_AUTH_TOKEN] = value
        updateForm {
            it.copy(
                authToken = value,
                isAuthTokenValid = true,
                authTokenError = null,
                isModified = isModified(it.serverUrl, value)
            )
        }
    }

    fun updateConnection() {
        if (!canStartOperation()) return
        if (!validateInputs()) return
        val form = _uiState.value.form
        val normalizedUrl = ValidationUtils.normalizeUrl(form.serverUrl)
        operationJob = viewModelScope.launch {
            Logger.i(TAG, "Saving settings and reconnecting to ${Logger.sanitizeUrl(normalizedUrl)}")
            _uiState.value = SettingsUiState.Connecting(
                form = form,
                connectionState = connectionState.value,
                canDisconnect = connectionState.value !is ConnectionState.Disconnected,
                isTesting = false
            )
            val result = connectToServerUseCase(normalizedUrl, form.authToken.trim(), persistSettings = true)
            result.onSuccess {
                val trimmedToken = form.authToken.trim()
                originalServerUrl = normalizedUrl
                originalAuthToken = trimmedToken
                _storedServerUrl.value = normalizedUrl
                _storedAuthToken.value = trimmedToken
                savedStateHandle[KEY_SERVER_URL] = normalizedUrl
                savedStateHandle[KEY_AUTH_TOKEN] = trimmedToken
                setFormState(
                    form.copy(
                        serverUrl = normalizedUrl,
                        authToken = trimmedToken,
                        isModified = false,
                        serverUrlError = null
                    )
                )
                emitEvent(R.string.message_settings_saved)
                _uiState.value = SettingsUiState.Success(
                    form = _uiState.value.form,
                    connectionState = connectionState.value,
                    canDisconnect = connectionState.value !is ConnectionState.Disconnected,
                    message = context.getString(R.string.status_connected)
                )
            }.onFailure { error ->
                val message = mapErrorMessage(error)
                updateStateForError(message)
                emitEvent(message)
            }
        }
    }

    fun testConnection() {
        if (!canStartOperation()) return
        if (!validateInputs()) return
        val form = _uiState.value.form
        val normalizedUrl = ValidationUtils.normalizeUrl(form.serverUrl)
        operationJob = viewModelScope.launch {
            Logger.i(TAG, "Testing connection to ${Logger.sanitizeUrl(normalizedUrl)}")
            _uiState.value = SettingsUiState.Connecting(
                form = form,
                connectionState = connectionState.value,
                canDisconnect = connectionState.value !is ConnectionState.Disconnected,
                isTesting = true
            )
            val result = connectToServerUseCase(normalizedUrl, form.authToken.trim(), persistSettings = false)
            result.onSuccess {
                emitEvent(R.string.message_connection_test_success)
                _uiState.value = SettingsUiState.Success(
                    form = form,
                    connectionState = connectionState.value,
                    canDisconnect = connectionState.value !is ConnectionState.Disconnected,
                    message = context.getString(R.string.status_connected)
                )
            }.onFailure { error ->
                val message = mapErrorMessage(error)
                emitEvent(R.string.message_connection_test_failed)
                updateStateForError(message)
            }
        }
    }

    fun disconnect() {
        if (!canStartOperation()) return
        operationJob = viewModelScope.launch {
            Logger.i(TAG, "Disconnecting from server")
            disconnectFromServerUseCase()
        }
    }

    fun clearSettings() {
        if (!canStartOperation()) return
        operationJob = viewModelScope.launch {
            Logger.w(TAG, "Clearing saved settings")
            settingsDataStore.clearSettings()
            disconnectFromServerUseCase()
            originalServerUrl = ""
            originalAuthToken = ""
            _storedServerUrl.value = ""
            _storedAuthToken.value = ""
            setFormState(
                SettingsFormState(
                    serverUrl = "",
                    authToken = "",
                    isModified = false
                )
            )
            savedStateHandle[KEY_SERVER_URL] = ""
            savedStateHandle[KEY_AUTH_TOKEN] = ""
        }
    }

    fun clearError() {
        val current = _uiState.value
        if (current is SettingsUiState.Error) {
            _uiState.value = SettingsUiState.Initial(
                form = current.form,
                connectionState = current.connectionState,
                canDisconnect = current.canDisconnect
            )
        }
    }

    private fun validateInputs(): Boolean {
        _uiState.value = SettingsUiState.Validating(
            form = _uiState.value.form,
            connectionState = connectionState.value,
            canDisconnect = connectionState.value !is ConnectionState.Disconnected
        )
        val validation = ValidationUtils.validateServerUrl(_uiState.value.form.serverUrl)
        val errorMessage = validation.error?.let { errorMessageFor(it) }
        if (!validation.isValid) {
            Logger.w(TAG, "Validation failed: ${validation.error}")
        }
        updateForm {
            it.copy(
                isServerUrlValid = validation.isValid,
                serverUrlError = errorMessage
            )
        }
        if (!validation.isValid) {
            _uiState.value = SettingsUiState.Initial(
                form = _uiState.value.form,
                connectionState = connectionState.value,
                canDisconnect = connectionState.value !is ConnectionState.Disconnected
            )
        }
        return validation.isValid
    }

    private fun updateForm(transform: (SettingsFormState) -> SettingsFormState) {
        _uiState.update { current ->
            val updatedForm = transform(current.form)
            current.withForm(updatedForm)
        }
    }

    private fun setFormState(form: SettingsFormState) {
        _uiState.update { current ->
            current.withForm(form)
        }
    }

    private fun updateStateForError(message: String) {
        _uiState.update { current ->
            SettingsUiState.Error(
                form = current.form,
                connectionState = current.connectionState,
                canDisconnect = current.canDisconnect,
                message = message
            )
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
        if ("authentication" in lowered || "unauthorized" in lowered || "forbidden" in lowered) {
            return context.getString(R.string.status_auth_failed)
        }
        if (networkError is com.harmonixia.android.util.NetworkError.AuthenticationError) {
            return context.getString(R.string.status_auth_failed)
        }
        if (throwable is TimeoutCancellationException || "timeout" in lowered) {
            return context.getString(R.string.error_connection_timeout)
        }
        return when (networkError) {
            is com.harmonixia.android.util.NetworkError.TimeoutError ->
                context.getString(R.string.error_connection_timeout)
            else -> context.getString(R.string.status_connection_failed)
        }
    }

    private fun emitEvent(messageResId: Int) {
        _events.tryEmit(SettingsUiEvent.ShowSnackbar(context.getString(messageResId)))
    }

    private fun emitEvent(message: String) {
        _events.tryEmit(SettingsUiEvent.ShowSnackbar(message))
    }

    private fun canStartOperation(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (operationJob?.isActive == true) return false
        if (now - lastOperationTimestamp < OPERATION_DEBOUNCE_MS) return false
        lastOperationTimestamp = now
        return true
    }

    private fun isModified(serverUrl: String, authToken: String): Boolean {
        return serverUrl != originalServerUrl || authToken != originalAuthToken
    }

    private fun SettingsUiState.withForm(form: SettingsFormState): SettingsUiState {
        return when (this) {
            is SettingsUiState.Initial -> copy(form = form)
            is SettingsUiState.Validating -> copy(form = form)
            is SettingsUiState.Connecting -> copy(form = form)
            is SettingsUiState.Success -> copy(form = form)
            is SettingsUiState.Error -> copy(form = form)
        }
    }

    private fun SettingsUiState.withConnectionState(
        state: ConnectionState,
        canDisconnect: Boolean
    ): SettingsUiState {
        return when (this) {
            is SettingsUiState.Initial -> copy(connectionState = state, canDisconnect = canDisconnect)
            is SettingsUiState.Validating -> copy(connectionState = state, canDisconnect = canDisconnect)
            is SettingsUiState.Connecting -> copy(connectionState = state, canDisconnect = canDisconnect)
            is SettingsUiState.Success -> copy(connectionState = state, canDisconnect = canDisconnect)
            is SettingsUiState.Error -> copy(connectionState = state, canDisconnect = canDisconnect)
        }
    }

    private fun loadFormState(): SettingsFormState {
        val serverUrl = savedStateHandle.get<String>(KEY_SERVER_URL).orEmpty()
        val authToken = savedStateHandle.get<String>(KEY_AUTH_TOKEN).orEmpty()
        return SettingsFormState(serverUrl = serverUrl, authToken = authToken)
    }

    private companion object {
        private const val TAG = "SettingsViewModel"
        private const val OPERATION_DEBOUNCE_MS = 1_000L
        private const val KEY_SERVER_URL = "settings_server_url"
        private const val KEY_AUTH_TOKEN = "settings_auth_token"
    }
}
