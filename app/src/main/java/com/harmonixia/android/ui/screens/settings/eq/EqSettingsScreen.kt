package com.harmonixia.android.ui.screens.settings.eq

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harmonixia.android.R
import com.harmonixia.android.ui.components.EqGraphCanvas
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.PresetDetailsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: EqSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredPresets by viewModel.filteredPresets.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()
    val eqSettings by viewModel.eqSettings.collectAsStateWithLifecycle()
    val presetDetails by viewModel.presetDetails.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.eq_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        EqSettingsScreenContent(
            uiState = uiState,
            searchQuery = searchQuery,
            filteredPresets = filteredPresets,
            selectedPreset = selectedPreset,
            selectedPresetId = selectedPreset?.id,
            eqEnabled = eqSettings.enabled,
            filters = selectedPreset?.filters.orEmpty(),
            presetDetails = presetDetails,
            onSearchQueryChange = viewModel::searchPresets,
            onPresetSelected = viewModel::selectPreset,
            onApplyPreset = viewModel::applyPreset,
            onToggleEnabled = viewModel::toggleEq,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun EqSettingsScreenContent(
    uiState: EqSettingsUiState,
    searchQuery: String,
    filteredPresets: List<com.harmonixia.android.domain.model.EqPreset>,
    selectedPreset: com.harmonixia.android.domain.model.EqPreset?,
    selectedPresetId: String?,
    eqEnabled: Boolean,
    filters: List<com.harmonixia.android.domain.model.EqFilter>,
    presetDetails: com.harmonixia.android.domain.model.EqPresetDetails?,
    onSearchQueryChange: (String) -> Unit,
    onPresetSelected: (String) -> Unit,
    onApplyPreset: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = uiState is EqSettingsUiState.Loading
    val errorMessage = (uiState as? EqSettingsUiState.Error)?.message
    val placeholderText = when {
        isLoading -> stringResource(R.string.eq_loading_presets)
        selectedPresetId == null -> stringResource(R.string.eq_select_preset_hint)
        else -> null
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.eq_enable_label),
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }
        }

        if (errorMessage != null) {
            item {
                ErrorCard(
                    message = errorMessage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text(text = stringResource(R.string.eq_search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.eq_preset_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            items(filteredPresets, key = { it.id }) { preset ->
                                val isSelected = preset.id == selectedPresetId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPresetSelected(preset.id) }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        style = if (isSelected) {
                                            MaterialTheme.typography.bodyMedium
                                        } else {
                                            MaterialTheme.typography.bodySmall
                                        },
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onApplyPreset,
                enabled = selectedPresetId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.eq_apply_button))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                EqGraphCanvas(
                    filters = filters,
                    width = 320.dp,
                    height = 200.dp,
                    placeholder = placeholderText,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.eq_preset_details_title),
                style = MaterialTheme.typography.titleMedium
            )
        }

        item {
            PresetDetailsCard(
                preset = selectedPreset,
                details = presetDetails,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            AttributionCard()
        }
    }
}

@Composable
private fun AttributionCard() {
    val uriHandler = LocalUriHandler.current
    val opraUrl = "https://github.com/opra-project/OPRA"
    val ladspaUrl = "https://github.com/pulseaudio-equalizer-ladspa/equalizer"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.eq_attribution_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "EQ Presets: OPRA (Open Parametric Room Acoustics)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Licensed under CC BY-SA 4.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Data sources: AutoEQ, oratory1990, and community contributors",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ClickableText(
                text = AnnotatedString(opraUrl),
                onClick = { uriHandler.openUri(opraUrl) },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "EQ Implementation: Concepts from pulseaudio-equalizer-ladspa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Licensed under GPL-3.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ClickableText(
                text = AnnotatedString(ladspaUrl),
                onClick = { uriHandler.openUri(ladspaUrl) },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
