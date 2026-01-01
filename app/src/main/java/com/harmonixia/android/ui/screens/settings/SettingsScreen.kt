package com.harmonixia.android.ui.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harmonixia.android.R
import com.harmonixia.android.data.local.LocalMediaScanner
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.AuthMethod
import com.harmonixia.android.ui.components.ConnectionStatusIndicator
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.LoadingButton
import com.harmonixia.android.ui.screens.settings.eq.EqSettingsScreenContent
import com.harmonixia.android.ui.screens.settings.eq.EqSettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPerformanceSettings: () -> Unit,
    initialTab: SettingsTab? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    eqViewModel: EqSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val localMediaFolderUri by viewModel.localMediaFolderUri.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            viewModel.updateLocalMediaFolder(uri.toString())
        }
    }

    LaunchedEffect(initialTab) {
        viewModel.setInitialTab(initialTab)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = viewModel::selectTab,
        snackbarHostState = snackbarHostState,
        localMediaFolderUri = localMediaFolderUri,
        localMediaScanState = uiState.localMediaScanState,
        onNavigateBack = onNavigateBack,
        onNavigateToPerformanceSettings = onNavigateToPerformanceSettings,
        onServerUrlChange = viewModel::updateServerUrl,
        onAuthTokenChange = viewModel::updateAuthToken,
        onAuthMethodChange = viewModel::updateAuthMethod,
        onUsernameChange = viewModel::updateUsername,
        onPasswordChange = viewModel::updatePassword,
        onTestConnection = viewModel::testConnection,
        onSaveConnection = viewModel::updateConnection,
        onDisconnect = viewModel::disconnect,
        onClearSettings = viewModel::clearSettings,
        onClearError = viewModel::clearError,
        onSelectFolder = { folderPickerLauncher.launch(null) },
        onScanLocalMedia = viewModel::scanLocalMedia,
        eqViewModel = eqViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    localMediaFolderUri: String,
    localMediaScanState: LocalMediaScanState,
    onNavigateBack: () -> Unit,
    onNavigateToPerformanceSettings: () -> Unit,
    onServerUrlChange: (String) -> Unit,
    onAuthTokenChange: (String) -> Unit,
    onAuthMethodChange: (AuthMethod) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSaveConnection: () -> Unit,
    onDisconnect: () -> Unit,
    onClearSettings: () -> Unit,
    onClearError: () -> Unit,
    onSelectFolder: () -> Unit,
    onScanLocalMedia: () -> Unit,
    eqViewModel: EqSettingsViewModel? = null
) {
    val isConnecting = uiState is SettingsUiState.Connecting
    val isTesting = (uiState as? SettingsUiState.Connecting)?.isTesting == true
    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    val isWideLayout = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val tabItems = listOf(
        SettingsTabItem(
            tab = SettingsTab.CONNECTION,
            labelResId = R.string.settings_tab_connection,
            icon = Icons.Outlined.Dns
        ),
        SettingsTabItem(
            tab = SettingsTab.EQUALIZER,
            labelResId = R.string.settings_tab_equalizer,
            icon = Icons.Outlined.Settings
        ),
        SettingsTabItem(
            tab = SettingsTab.LOCAL_MEDIA,
            labelResId = R.string.settings_tab_local_media,
            icon = Icons.Outlined.MusicNote
        )
    )

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
            val tabContent: @Composable (Modifier) -> Unit = { modifier ->
                Box(modifier = modifier) {
                    when (selectedTab) {
                        SettingsTab.CONNECTION -> ConnectionTabContent(
                            uiState = uiState,
                            isConnecting = isConnecting,
                            isTesting = isTesting,
                            onServerUrlChange = onServerUrlChange,
                            onAuthTokenChange = onAuthTokenChange,
                            onAuthMethodChange = onAuthMethodChange,
                            onUsernameChange = onUsernameChange,
                            onPasswordChange = onPasswordChange,
                            onTestConnection = onTestConnection,
                            onSaveConnection = onSaveConnection,
                            onDisconnect = onDisconnect,
                            onClearSettings = onClearSettings,
                            onClearError = onClearError
                        )
                        SettingsTab.EQUALIZER -> {
                            if (eqViewModel != null) {
                                EqualizerTabContent(
                                    eqViewModel = eqViewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        SettingsTab.LOCAL_MEDIA -> LocalMediaTabContent(
                            localMediaFolderUri = localMediaFolderUri,
                            localMediaScanState = localMediaScanState,
                            onSelectFolder = onSelectFolder,
                            onScanLocalMedia = onScanLocalMedia
                        )
                    }
                }
            }

            if (isWideLayout) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        tabItems.forEach { tabItem ->
                            val label = stringResource(tabItem.labelResId)
                            NavigationRailItem(
                                selected = selectedTab == tabItem.tab,
                                onClick = { onTabSelected(tabItem.tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabItem.icon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(text = label) }
                            )
                        }
                    }
                    tabContent(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = selectedTab.ordinal) {
                        tabItems.forEach { tabItem ->
                            Tab(
                                selected = selectedTab == tabItem.tab,
                                onClick = { onTabSelected(tabItem.tab) },
                                text = { Text(text = stringResource(tabItem.labelResId)) }
                            )
                        }
                    }
                    tabContent(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
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
        }
    }
}

@Composable
private fun ConnectionTabContent(
    uiState: SettingsUiState,
    isConnecting: Boolean,
    isTesting: Boolean,
    onServerUrlChange: (String) -> Unit,
    onAuthTokenChange: (String) -> Unit,
    onAuthMethodChange: (AuthMethod) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSaveConnection: () -> Unit,
    onDisconnect: () -> Unit,
    onClearSettings: () -> Unit,
    onClearError: () -> Unit
) {
    val form = uiState.form
    val errorMessage = (uiState as? SettingsUiState.Error)?.message
    val isSuccess = uiState is SettingsUiState.Success
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val tokenFocusRequester = remember { FocusRequester() }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isTokenVisible by rememberSaveable { mutableStateOf(false) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val connectionStatusText = stringResource(
        if (uiState.connectionState is ConnectionState.Connected) {
            R.string.status_music_assistant_connected
        } else {
            R.string.status_music_assistant_disconnected
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionStatusIndicator(connectionState = uiState.connectionState)
            Text(
                text = connectionStatusText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.settings_edit_connection))
        }

        if (isExpanded) {
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
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.section_authentication),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(12.dp))

            AuthMethodSelector(
                authMethod = form.authMethod,
                onAuthMethodChange = onAuthMethodChange,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (form.authMethod) {
                AuthMethod.TOKEN -> {
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
                }
                AuthMethod.USERNAME_PASSWORD -> {
                    val usernameFocusRequester = remember { FocusRequester() }
                    val passwordFocusRequester = remember { FocusRequester() }
                    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

                    OutlinedTextField(
                        value = form.username,
                        onValueChange = onUsernameChange,
                        label = { Text(text = stringResource(R.string.label_username)) },
                        placeholder = { Text(text = stringResource(R.string.placeholder_username)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = stringResource(R.string.content_desc_username_icon)
                            )
                        },
                        isError = form.usernameError != null,
                        supportingText = {
                            if (form.usernameError != null) {
                                Text(
                                    text = form.usernameError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusRequester.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(usernameFocusRequester)
                            .testTag("settings_username")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = form.password,
                        onValueChange = onPasswordChange,
                        label = { Text(text = stringResource(R.string.label_password)) },
                        placeholder = { Text(text = stringResource(R.string.placeholder_password)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = stringResource(R.string.content_desc_password_icon)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = stringResource(
                                        if (isPasswordVisible) {
                                            R.string.content_desc_password_visibility_off
                                        } else {
                                            R.string.content_desc_password_visibility_on
                                        }
                                    )
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        isError = form.passwordError != null,
                        supportingText = {
                            if (form.passwordError != null) {
                                Text(
                                    text = form.passwordError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester)
                            .testTag("settings_password")
                    )
                }
            }

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

@Composable
private fun EqualizerTabContent(
    eqViewModel: EqSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by eqViewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by eqViewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredPresets by eqViewModel.filteredPresets.collectAsStateWithLifecycle()
    val selectedPreset by eqViewModel.selectedPreset.collectAsStateWithLifecycle()
    val eqSettings by eqViewModel.eqSettings.collectAsStateWithLifecycle()
    val presetDetails by eqViewModel.presetDetails.collectAsStateWithLifecycle()

    EqSettingsScreenContent(
        uiState = uiState,
        searchQuery = searchQuery,
        filteredPresets = filteredPresets,
        selectedPreset = selectedPreset,
        selectedPresetId = selectedPreset?.id,
        eqEnabled = eqSettings.enabled,
        filters = selectedPreset?.filters.orEmpty(),
        presetDetails = presetDetails,
        onSearchQueryChange = eqViewModel::searchPresets,
        onPresetSelected = eqViewModel::selectPreset,
        onApplyPreset = eqViewModel::applyPreset,
        onToggleEnabled = eqViewModel::toggleEq,
        modifier = modifier
    )
}

@Composable
private fun LocalMediaTabContent(
    localMediaFolderUri: String,
    localMediaScanState: LocalMediaScanState,
    onSelectFolder: () -> Unit,
    onScanLocalMedia: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.section_local_media),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.local_media_folder_label),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = localMediaFolderUri.ifBlank {
                stringResource(R.string.local_media_no_folder_selected)
            },
            onValueChange = {},
            readOnly = true,
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_local_media_folder")
                .semantics {
                    contentDescription = context.getString(R.string.content_desc_local_media_folder)
                }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSelectFolder,
                enabled = !localMediaScanState.isScanning,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.local_media_select_folder))
            }

            LoadingButton(
                text = stringResource(R.string.local_media_refresh),
                onClick = onScanLocalMedia,
                enabled = localMediaFolderUri.isNotBlank() && !localMediaScanState.isScanning,
                isLoading = localMediaScanState.isScanning,
                modifier = Modifier.weight(1f),
                testTag = "settings_scan_local_media"
            )
        }

        when (val progress = localMediaScanState.progress) {
            is LocalMediaScanner.ScanProgress.Scanning -> {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.current.toFloat() / progress.total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.local_media_scanning,
                        progress.current,
                        progress.total
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is LocalMediaScanner.ScanProgress.Complete -> {
                Spacer(modifier = Modifier.height(12.dp))
                StatusCard(
                    message = stringResource(
                        R.string.local_media_scan_complete,
                        progress.result.tracksAdded,
                        progress.result.albumsAdded,
                        progress.result.artistsAdded
                    )
                )
            }
            is LocalMediaScanner.ScanProgress.Error -> {
                Spacer(modifier = Modifier.height(12.dp))
                ErrorCard(
                    message = stringResource(
                        R.string.local_media_scan_error,
                        progress.message
                    ),
                    onDismiss = { /* Clear error state if needed */ },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LocalMediaScanner.ScanProgress.Idle -> {
                // No status to show
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthMethodSelector(
    authMethod: AuthMethod,
    onAuthMethodChange: (AuthMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        AuthMethod.TOKEN to stringResource(R.string.label_auth_method_token),
        AuthMethod.USERNAME_PASSWORD to stringResource(R.string.label_auth_method_username_password)
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (method, label) ->
            SegmentedButton(
                selected = authMethod == method,
                onClick = { onAuthMethodChange(method) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.testTag(
                    if (method == AuthMethod.TOKEN) {
                        "settings_auth_method_token"
                    } else {
                        "settings_auth_method_username_password"
                    }
                )
            ) {
                Text(text = label)
            }
        }
    }
}

private data class SettingsTabItem(
    val tab: SettingsTab,
    val labelResId: Int,
    val icon: ImageVector
)
