package com.harmonixia.android.ui.screens.settings.eq

import com.harmonixia.android.domain.model.EqPreset
import com.harmonixia.android.domain.model.EqSettings

sealed class EqSettingsUiState {
    data object Loading : EqSettingsUiState()

    data class Success(
        val presets: List<EqPreset>,
        val selectedPreset: EqPreset?,
        val settings: EqSettings
    ) : EqSettingsUiState()

    data class Error(val message: String) : EqSettingsUiState()
}
