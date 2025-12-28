package com.harmonixia.android.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harmonixia.android.R
import com.harmonixia.android.ui.components.ConnectionStatusIndicator
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.LoadingButton

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEqSettings: () -> Unit,
    onNavigateToPerformanceSettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val storedServerUrl by viewModel.storedServerUrl.collectAsStateWithLifecycle()
    val storedAuthToken by viewModel.storedAuthToken.collectAsStateWithLifecycle()
    val eqSettings by viewModel.eqSettings.collectAsStateWithLifecycle()
    val eqPresetName by viewModel.eqPresetName.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        storedServerUrl = storedServerUrl,
        storedAuthToken = storedAuthToken,
        snackbarHostState = snackbarHostState,
        eqEnabled = eqSettings.enabled,
        eqPresetName = eqPresetName,
        onNavigateBack = onNavigateBack,
        onNavigateToEqSettings = onNavigateToEqSettings,
        onNavigateToPerformanceSettings = onNavigateToPerformanceSettings,
        onServerUrlChange = viewModel::updateServerUrl,
        onAuthTokenChange = viewModel::updateAuthToken,
        onTestConnection = viewModel::testConnection,
        onSaveConnection = viewModel::updateConnection,
        onDisconnect = viewModel::disconnect,
        onClearSettings = viewModel::clearSettings,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    storedServerUrl: String,
    storedAuthToken: String,
    snackbarHostState: SnackbarHostState,
    eqEnabled: Boolean,
    eqPresetName: String?,
    onNavigateBack: () -> Unit,
    onNavigateToEqSettings: () -> Unit,
    onNavigateToPerformanceSettings: () -> Unit,
    onServerUrlChange: (String) -> Unit,
    onAuthTokenChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSaveConnection: () -> Unit,
    onDisconnect: () -> Unit,
    onClearSettings: () -> Unit,
    onClearError: () -> Unit
) {
    val form = uiState.form
    val isConnecting = uiState is SettingsUiState.Connecting
    val isTesting = (uiState as? SettingsUiState.Connecting)?.isTesting == true
    val isSuccess = uiState is SettingsUiState.Success
    val errorMessage = (uiState as? SettingsUiState.Error)?.message
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val tokenFocusRequester = remember { FocusRequester() }
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    var isTokenVisible by rememberSaveable { mutableStateOf(false) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isConnecting) {
        // Disable back navigation during connection attempts.
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { onNavigateToPerformanceSettings() }
                            )
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isConnecting) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.section_connection_status),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                ConnectionStatusIndicator(
                    connectionState = uiState.connectionState,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isConnecting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressRow()
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ErrorCard(
                        message = errorMessage,
                        onDismiss = onClearError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isSuccess) {
                    Spacer(modifier = Modifier.height(16.dp))
                    StatusCard(message = (uiState as SettingsUiState.Success).message)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.label_current_server_url),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = storedServerUrl,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_current_server_url")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.label_current_access_token),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = storedAuthToken,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_current_auth_token")
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                val connectionSettingsLabel = stringResource(R.string.section_connection_settings)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = connectionSettingsLabel },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = connectionSettingsLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = stringResource(
                                if (isExpanded) {
                                    R.string.content_desc_collapse_section
                                } else {
                                    R.string.content_desc_expand_section
                                }
                            )
                        )
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = form.serverUrl,
                        onValueChange = onServerUrlChange,
                        label = { Text(text = stringResource(R.string.label_server_url)) },
                        placeholder = { Text(text = stringResource(R.string.placeholder_server_url)) },
                        isError = form.serverUrlError != null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Dns,
                                contentDescription = stringResource(R.string.content_desc_server_icon)
                            )
                        },
                        supportingText = {
                            if (form.serverUrlError != null) {
                                Text(
                                    text = form.serverUrlError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { tokenFocusRequester.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_server_url")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = form.authToken,
                        onValueChange = onAuthTokenChange,
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = stringResource(R.string.label_access_token))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.label_optional_access_token),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        },
                        placeholder = { Text(text = stringResource(R.string.placeholder_access_token_optional)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = stringResource(R.string.content_desc_token_icon)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Icon(
                                    imageVector = if (isTokenVisible) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = stringResource(
                                        if (isTokenVisible) {
                                            R.string.content_desc_visibility_off
                                        } else {
                                            R.string.content_desc_visibility_on
                                        }
                                    )
                                )
                            }
                        },
                        visualTransformation = if (isTokenVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (form.isFormValid) {
                                    onTestConnection()
                                }
                            }
                        ),
                        supportingText = {
                            Text(
                                text = stringResource(R.string.placeholder_access_token_optional),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(tokenFocusRequester)
                            .testTag("settings_auth_token")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LoadingButton(
                            text = stringResource(R.string.action_test_connection),
                            onClick = onTestConnection,
                            enabled = form.isFormValid,
                            isLoading = isConnecting && isTesting,
                            modifier = Modifier.weight(1f),
                            testTag = "settings_test_connection"
                        )
                        LoadingButton(
                            text = stringResource(R.string.action_save_reconnect),
                            onClick = onSaveConnection,
                            enabled = form.isFormValid && form.isModified,
                            isLoading = isConnecting && !isTesting,
                            modifier = Modifier.weight(1f),
                            testTag = "settings_save_reconnect"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.eq_settings_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presetLabel = stringResource(R.string.eq_preset_label)
                    val presetValue = eqPresetName ?: stringResource(R.string.eq_no_preset_selected)
                    Text(
                        text = "$presetLabel: $presetValue",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (eqEnabled) {
                        Badge {
                            Text(text = stringResource(R.string.eq_enabled_badge))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateToEqSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.eq_configure_button))
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.canDisconnect) {
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_disconnect")
                    ) {
                        Text(text = stringResource(R.string.action_disconnect))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_clear_settings")
                ) {
                    Text(text = stringResource(R.string.action_clear_settings))
                }
            }

            if (isConnecting) {
                val connectingLabel = stringResource(R.string.status_connecting)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                        .pointerInput(Unit) {}
                        .semantics {
                            contentDescription = connectingLabel
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text(text = stringResource(R.string.message_clear_settings_confirm)) },
                    text = { Text(text = stringResource(R.string.message_clear_settings_body)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showClearDialog = false
                                onClearSettings()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(text = stringResource(R.string.action_clear_settings))
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showClearDialog = false }) {
                            Text(text = stringResource(R.string.action_back))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LinearProgressRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = stringResource(R.string.status_connecting))
    }
}

@Composable
private fun StatusCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_success"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = stringResource(R.string.content_desc_success_icon),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
