package com.harmonixia.android.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.LoadingButton

@Composable
fun OnboardingScreen(
    onNavigateToHome: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is OnboardingUiState.Success) {
            onNavigateToHome()
        }
    }

    OnboardingScreenContent(
        uiState = uiState,
        connectionState = connectionState,
        onServerUrlChange = viewModel::updateServerUrl,
        onAuthTokenChange = viewModel::updateAuthToken,
        onConnect = viewModel::connect,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OnboardingScreenContent(
    uiState: OnboardingUiState,
    connectionState: ConnectionState,
    onServerUrlChange: (String) -> Unit,
    onAuthTokenChange: (String) -> Unit,
    onConnect: () -> Unit,
    onClearError: () -> Unit
) {
    val form = uiState.form
    val isConnecting = uiState is OnboardingUiState.Connecting
    val isSuccess = uiState is OnboardingUiState.Success
    val errorMessage = (uiState as? OnboardingUiState.Error)?.message
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val tokenFocusRequester = remember { FocusRequester() }
    var isTokenVisible by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isConnecting) {
        // Disable back navigation during connection attempts.
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.onboarding_title)) })
        }
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = stringResource(R.string.message_welcome),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.help_onboarding),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = form.serverUrl,
                    onValueChange = onServerUrlChange,
                    label = { Text(text = stringResource(R.string.label_server_url)) },
                    placeholder = { Text(text = stringResource(R.string.placeholder_server_url)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Dns,
                            contentDescription = stringResource(R.string.content_desc_server_icon)
                        )
                    },
                    isError = form.serverUrlError != null,
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
                        .testTag("onboarding_server_url")
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
                        IconButton(
                            onClick = { isTokenVisible = !isTokenVisible }
                        ) {
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
                                onConnect()
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
                        .testTag("onboarding_auth_token")
                )

                Spacer(modifier = Modifier.height(24.dp))

                LoadingButton(
                    text = stringResource(R.string.action_connect),
                    onClick = onConnect,
                    enabled = form.isFormValid,
                    isLoading = isConnecting,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "onboarding_connect"
                )

                if (isConnecting || connectionState is ConnectionState.Connecting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_progress")
                    )
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_success"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                        ) {
                            RowWithIcon(
                                text = stringResource(R.string.status_connected),
                                icon = Icons.Outlined.CheckCircle,
                                contentDescription = stringResource(R.string.content_desc_success_icon)
                            )
                        }
                    }
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
private fun RowWithIcon(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
