package com.harmonixia.android.ui.screens.settings

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.data.local.LocalMediaScanner
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.AuthMethod
import com.harmonixia.android.domain.model.EqSettings
import com.harmonixia.android.domain.repository.EqPresetRepository
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.usecase.ConnectToServerUseCase
import com.harmonixia.android.domain.usecase.DisconnectFromServerUseCase
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.ui.navigation.Screen
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
    private val localMediaRepository: LocalMediaRepository,
    private val localMediaScanner: LocalMediaScanner,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val connectionState: StateFlow<ConnectionState> = getConnectionStateUseCase()

    private val _selectedTab = MutableStateFlow(SettingsTab.CONNECTION)
    val selectedTab: StateFlow<SettingsTab> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow<SettingsUiState>(
        SettingsUiState.Initial(
            form = loadFormState(),
            connectionState = connectionState.value,
            canDisconnect = false,
            localMediaScanState = LocalMediaScanState(),
            selectedTab = _selectedTab.value
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _storedServerUrl = MutableStateFlow("")
    val storedServerUrl: StateFlow<String> = _storedServerUrl.asStateFlow()

    private val _storedAuthToken = MutableStateFlow("")
    val storedAuthToken: StateFlow<String> = _storedAuthToken.asStateFlow()

    private val _storedAuthMethod = MutableStateFlow(AuthMethod.TOKEN)
    val storedAuthMethod: StateFlow<AuthMethod> = _storedAuthMethod.asStateFlow()

    private val _storedUsername = MutableStateFlow("")
    val storedUsername: StateFlow<String> = _storedUsername.asStateFlow()

    private val _storedPassword = MutableStateFlow("")
    val storedPassword: StateFlow<String> = _storedPassword.asStateFlow()

    private val _localMediaFolderUri = MutableStateFlow("")
    val localMediaFolderUri: StateFlow<String> = _localMediaFolderUri.asStateFlow()

    private val _localMediaTrackCount = MutableStateFlow(0)
    val localMediaTrackCount: StateFlow<Int> = _localMediaTrackCount.asStateFlow()

    private val _events = MutableSharedFlow<SettingsUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val _eqSettings = MutableStateFlow(EqSettings())
    val eqSettings: StateFlow<EqSettings> = _eqSettings.asStateFlow()

    private val _eqPresetName = MutableStateFlow<String?>(null)
    val eqPresetName: StateFlow<String?> = _eqPresetName.asStateFlow()

    private var originalServerUrl: String = ""
    private var originalAuthToken: String = ""
    private var originalAuthMethod: AuthMethod = AuthMethod.TOKEN
    private var originalUsername: String = ""
    private var originalPassword: String = ""
    private var operationJob: Job? = null
    private var lastOperationTimestamp: Long = 0L

    init {
        restoreInitialTab()
        viewModelScope.launch {
            combine(
                settingsDataStore.getServerUrl(),
                settingsDataStore.getAuthToken(),
                settingsDataStore.getAuthMethod(),
                settingsDataStore.getUsername(),
                settingsDataStore.getPassword()
            ) { url, token, method, user, pass ->
                SettingsData(url, token, method, user, pass)
            }.collect { data ->
                originalServerUrl = data.url
                originalAuthToken = data.token
                originalAuthMethod = data.method
                originalUsername = data.username
                originalPassword = data.password
                _storedServerUrl.value = data.url
                _storedAuthToken.value = data.token
                _storedAuthMethod.value = data.method
                _storedUsername.value = data.username
                _storedPassword.value = data.password
                val currentForm = _uiState.value.form
                val shouldReplace = !currentForm.isModified
                if (shouldReplace) {
                    setFormState(
                        currentForm.copy(
                            serverUrl = data.url,
                            authToken = data.token,
                            authMethod = data.method,
                            username = data.username,
                            password = data.password,
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
            settingsDataStore.getLocalMediaFolderUri().collect { uri ->
                _localMediaFolderUri.value = uri
            }
        }

        viewModelScope.launch {
            localMediaRepository.getTrackCount().collect { count ->
                _localMediaTrackCount.value = count
            }
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
            localMediaScanner.getScanProgress().collect { progress ->
                _uiState.update { current ->
                    current.withLocalMediaScanState(
                        LocalMediaScanState(
                            isScanning = progress is LocalMediaScanner.ScanProgress.Scanning,
                            progress = progress
                        )
                    )
                }
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

    private fun restoreInitialTab() {
        if (savedStateHandle.get<Boolean>(KEY_INITIAL_TAB_SET) == true) {
            return
        }
        val tabArg = savedStateHandle.get<String>(Screen.Settings.ARG_TAB)
        val tab = tabArg?.let { value ->
            runCatching { SettingsTab.valueOf(value) }.getOrNull()
        }
        if (tab != null) {
            selectTab(tab)
            savedStateHandle[KEY_INITIAL_TAB_SET] = true
        }
    }

    fun selectTab(tab: SettingsTab) {
        _selectedTab.value = tab
        _uiState.update { current ->
            current.withSelectedTab(tab)
        }
    }

    fun setInitialTab(tab: SettingsTab?) {
        if (savedStateHandle.get<Boolean>(KEY_INITIAL_TAB_SET) == true) {
            return
        }
        if (tab != null) {
            selectTab(tab)
            savedStateHandle[KEY_INITIAL_TAB_SET] = true
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
                isModified = isModified(value, it.authToken, it.authMethod, it.username, it.password)
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
                isModified = isModified(it.serverUrl, value, it.authMethod, it.username, it.password)
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
                        passwordError = null,
                        isModified = isModified(current.serverUrl, current.authToken, value, "", "")
                    )
                }
                AuthMethod.USERNAME_PASSWORD -> {
                    savedStateHandle[KEY_AUTH_TOKEN] = ""
                    current.copy(
                        authMethod = value,
                        authToken = "",
                        isAuthTokenValid = true,
                        authTokenError = null,
                        isModified = isModified(
                            current.serverUrl,
                            "",
                            value,
                            current.username,
                            current.password
                        )
                    )
                }
            }
        }
    }

    fun updateUsername(value: String) {
        savedStateHandle[KEY_USERNAME] = value
        val isValid = value.isNotBlank()
        val errorMessage = if (isValid) null else context.getString(R.string.error_username_required)
        updateForm {
            it.copy(
                username = value,
                isUsernameValid = isValid,
                usernameError = errorMessage,
                isModified = isModified(it.serverUrl, it.authToken, it.authMethod, value, it.password)
            )
        }
    }

    fun updatePassword(value: String) {
        savedStateHandle[KEY_PASSWORD] = value
        val isValid = value.isNotBlank()
        val errorMessage = if (isValid) null else context.getString(R.string.error_password_required)
        updateForm {
            it.copy(
                password = value,
                isPasswordValid = isValid,
                passwordError = errorMessage,
                isModified = isModified(it.serverUrl, it.authToken, it.authMethod, it.username, value)
            )
        }
    }

    fun updateLocalMediaFolder(uri: String) {
        viewModelScope.launch {
            val previousUri = _localMediaFolderUri.value
            settingsDataStore.saveLocalMediaFolderUri(uri)
            _localMediaFolderUri.value = uri
            emitEvent("Local media folder updated")
            if (uri.isNotBlank() && uri != previousUri) {
                startLocalMediaScan(uri)
            }
        }
    }

    fun scanLocalMedia() {
        startLocalMediaScan(_localMediaFolderUri.value)
    }

    private fun startLocalMediaScan(folderUri: String) {
        if (folderUri.isBlank()) {
            emitEvent("Please select a folder first")
            return
        }

        viewModelScope.launch {
            Logger.i(TAG, "Starting local media scan")
            val result = localMediaScanner.scanFolder(folderUri)
            result.onSuccess { scanResult ->
                val message = context.getString(
                    R.string.local_media_scan_complete,
                    scanResult.tracksAdded,
                    scanResult.albumsAdded,
                    scanResult.artistsAdded
                )
                emitEvent(message)
            }.onFailure { error ->
                val message = context.getString(
                    R.string.local_media_scan_error,
                    error.message ?: "Unknown error"
                )
                emitEvent(message)
            }
        }
    }

    fun updateConnection() {
        if (!canStartOperation()) return
        if (!validateInputs()) return
        val form = _uiState.value.form
        val normalizedUrl = ValidationUtils.normalizeUrl(form.serverUrl)
        operationJob = viewModelScope.launch {
            Logger.i(TAG, "Saving settings and reconnecting to ${Logger.sanitizeUrl(normalizedUrl)}")
            val current = _uiState.value
            _uiState.value = SettingsUiState.Connecting(
                form = form,
                connectionState = connectionState.value,
                canDisconnect = connectionState.value !is ConnectionState.Disconnected,
                localMediaScanState = current.localMediaScanState,
                selectedTab = current.selectedTab,
                isTesting = false
            )
            val result = connectToServerUseCase(
                normalizedUrl,
                form.authToken.trim(),
                form.authMethod,
                form.username.trim(),
                form.password.trim(),
                persistSettings = true
            )
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
                val updatedState = _uiState.value
                _uiState.value = SettingsUiState.Success(
                    form = updatedState.form,
                    connectionState = connectionState.value,
                    canDisconnect = connectionState.value !is ConnectionState.Disconnected,
                    localMediaScanState = updatedState.localMediaScanState,
                    selectedTab = updatedState.selectedTab,
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
            val current = _uiState.value
            _uiState.value = SettingsUiState.Connecting(
                form = form,
                connectionState = connectionState.value,
                canDisconnect = connectionState.value !is ConnectionState.Disconnected,
                localMediaScanState = current.localMediaScanState,
                selectedTab = current.selectedTab,
                isTesting = true
            )
            val result = connectToServerUseCase(
                normalizedUrl,
                form.authToken.trim(),
                form.authMethod,
                form.username.trim(),
                form.password.trim(),
                persistSettings = false
            )
            result.onSuccess {
                emitEvent(R.string.message_connection_test_success)
                val updatedState = _uiState.value
                _uiState.value = SettingsUiState.Success(
                    form = form,
                    connectionState = connectionState.value,
                    canDisconnect = connectionState.value !is ConnectionState.Disconnected,
                    localMediaScanState = updatedState.localMediaScanState,
                    selectedTab = updatedState.selectedTab,
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
            originalAuthMethod = AuthMethod.TOKEN
            originalUsername = ""
            originalPassword = ""
            _storedServerUrl.value = ""
            _storedAuthToken.value = ""
            _storedAuthMethod.value = AuthMethod.TOKEN
            _storedUsername.value = ""
            _storedPassword.value = ""
            setFormState(
                SettingsFormState(
                    serverUrl = "",
                    authToken = "",
                    authMethod = AuthMethod.TOKEN,
                    username = "",
                    password = "",
                    isModified = false
                )
            )
            savedStateHandle[KEY_SERVER_URL] = ""
            savedStateHandle[KEY_AUTH_TOKEN] = ""
            savedStateHandle[KEY_AUTH_METHOD] = AuthMethod.TOKEN
            savedStateHandle[KEY_USERNAME] = ""
            savedStateHandle[KEY_PASSWORD] = ""
        }
    }

    fun clearError() {
        val current = _uiState.value
        if (current is SettingsUiState.Error) {
            _uiState.value = SettingsUiState.Initial(
                form = current.form,
                connectionState = current.connectionState,
                canDisconnect = current.canDisconnect,
                localMediaScanState = current.localMediaScanState,
                selectedTab = current.selectedTab
            )
        }
    }

    private fun validateInputs(): Boolean {
        val current = _uiState.value
        _uiState.value = SettingsUiState.Validating(
            form = current.form,
            connectionState = connectionState.value,
            canDisconnect = connectionState.value !is ConnectionState.Disconnected,
            localMediaScanState = current.localMediaScanState,
            selectedTab = current.selectedTab
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
            val updatedState = _uiState.value
            _uiState.value = SettingsUiState.Initial(
                form = updatedState.form,
                connectionState = connectionState.value,
                canDisconnect = connectionState.value !is ConnectionState.Disconnected,
                localMediaScanState = updatedState.localMediaScanState,
                selectedTab = updatedState.selectedTab
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
                localMediaScanState = current.localMediaScanState,
                selectedTab = current.selectedTab,
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

    private fun isModified(
        serverUrl: String,
        authToken: String,
        authMethod: AuthMethod,
        username: String,
        password: String
    ): Boolean {
        return serverUrl != originalServerUrl ||
            authToken != originalAuthToken ||
            authMethod != originalAuthMethod ||
            username != originalUsername ||
            password != originalPassword
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

    private fun loadFormState(): SettingsFormState {
        val serverUrl = savedStateHandle.get<String>(KEY_SERVER_URL).orEmpty()
        val authToken = savedStateHandle.get<String>(KEY_AUTH_TOKEN).orEmpty()
        val authMethod = savedStateHandle.get<AuthMethod>(KEY_AUTH_METHOD)
            ?: savedStateHandle.get<String>(KEY_AUTH_METHOD)?.let { stored ->
                runCatching { AuthMethod.valueOf(stored) }.getOrNull()
            }
            ?: AuthMethod.TOKEN
        val username = savedStateHandle.get<String>(KEY_USERNAME).orEmpty()
        val password = savedStateHandle.get<String>(KEY_PASSWORD).orEmpty()
        return SettingsFormState(
            serverUrl = serverUrl,
            authToken = authToken,
            authMethod = authMethod,
            username = username,
            password = password
        )
    }

    private data class SettingsData(
        val url: String,
        val token: String,
        val method: AuthMethod,
        val username: String,
        val password: String
    )

    private companion object {
        private const val TAG = "SettingsViewModel"
        private const val OPERATION_DEBOUNCE_MS = 1_000L
        private const val KEY_SERVER_URL = "settings_server_url"
        private const val KEY_AUTH_TOKEN = "settings_auth_token"
        private const val KEY_AUTH_METHOD = "settings_auth_method"
        private const val KEY_USERNAME = "settings_username"
        private const val KEY_PASSWORD = "settings_password"
        private const val KEY_INITIAL_TAB_SET = "settings_initial_tab_set"
    }
}
