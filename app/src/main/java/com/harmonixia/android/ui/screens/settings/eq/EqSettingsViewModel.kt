package com.harmonixia.android.ui.screens.settings.eq

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.R
import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.domain.model.EqPreset
import com.harmonixia.android.domain.model.EqPresetDetails
import com.harmonixia.android.domain.model.EqSettings
import com.harmonixia.android.domain.usecase.ApplyEqPresetUseCase
import com.harmonixia.android.domain.usecase.GetEqSettingsUseCase
import com.harmonixia.android.domain.usecase.LoadEqPresetsUseCase
import com.harmonixia.android.domain.usecase.SearchEqPresetsUseCase
import com.harmonixia.android.service.playback.EqualizerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EqSettingsViewModel @Inject constructor(
    private val loadEqPresetsUseCase: LoadEqPresetsUseCase,
    private val searchEqPresetsUseCase: SearchEqPresetsUseCase,
    private val applyEqPresetUseCase: ApplyEqPresetUseCase,
    private val getEqSettingsUseCase: GetEqSettingsUseCase,
    private val eqDataStore: EqDataStore,
    private val equalizerManager: EqualizerManager,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _presets = MutableStateFlow<List<EqPreset>>(emptyList())
    val presetsFlow: StateFlow<List<EqPreset>> = _presets.asStateFlow()

    private val _searchQuery = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SEARCH_QUERY).orEmpty()
    )
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPresetId = MutableStateFlow<String?>(null)
    val selectedPreset: StateFlow<EqPreset?> = combine(_selectedPresetId, presetsFlow) { presetId, presets ->
        presets.firstOrNull { it.id == presetId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val eqSettings: StateFlow<EqSettings> = getEqSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, EqSettings())

    val presetDetails: StateFlow<EqPresetDetails?> = selectedPreset
        .combine(eqSettings) { preset, _ -> preset?.let { buildPresetDetails(it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _loading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val filteredPresets: StateFlow<List<EqPreset>> = _searchQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .combine(presetsFlow) { query, presets ->
            if (query.isBlank()) {
                presets
            } else {
                searchEqPresetsUseCase(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<EqSettingsUiState> = combine(
        _loading,
        _errorMessage,
        presetsFlow,
        selectedPreset,
        eqSettings
    ) { loading, error, presets, selected, settings ->
        when {
            error != null -> EqSettingsUiState.Error(error)
            loading -> EqSettingsUiState.Loading
            else -> EqSettingsUiState.Success(
                presets = presets,
                selectedPreset = selected,
                settings = settings
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EqSettingsUiState.Loading)

    init {
        viewModelScope.launch {
            eqSettings.collect { settings ->
                if (settings.selectedPresetId != _selectedPresetId.value) {
                    _selectedPresetId.value = settings.selectedPresetId
                }
            }
        }
        loadPresets()
    }

    fun loadPresets(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            loadEqPresetsUseCase(forceRefresh)
                .onSuccess { presets ->
                    _presets.value = presets
                }
                .onFailure {
                    _errorMessage.value = context.getString(R.string.eq_error_load_failed)
                }
            _loading.value = false
        }
    }

    fun searchPresets(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun selectPreset(id: String) {
        _selectedPresetId.value = id
    }

    fun applyPreset() {
        val presetId = _selectedPresetId.value ?: return
        viewModelScope.launch {
            applyEqPresetUseCase(presetId)
                .onFailure {
                    _errorMessage.value = context.getString(R.string.eq_error_apply_failed)
                }
        }
    }

    fun toggleEq(enabled: Boolean) {
        viewModelScope.launch {
            val current = eqSettings.value
            eqDataStore.saveEqSettings(current.copy(enabled = enabled))
            equalizerManager.setEnabled(enabled)
        }
    }

    private fun buildPresetDetails(preset: EqPreset): EqPresetDetails {
        val supportedBands = DEFAULT_SUPPORTED_BANDS
        val dropped = max(0, preset.filters.size - supportedBands)
        return EqPresetDetails(
            presetId = preset.id,
            filterCount = preset.filters.size,
            supportedBands = supportedBands,
            droppedFilters = dropped
        )
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val DEFAULT_SUPPORTED_BANDS = 5
        private const val KEY_SEARCH_QUERY = "eq_search_query"
    }
}
