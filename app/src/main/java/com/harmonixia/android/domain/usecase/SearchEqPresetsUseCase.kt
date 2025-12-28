package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.EqPreset
import com.harmonixia.android.domain.repository.EqPresetRepository

class SearchEqPresetsUseCase(
    private val repository: EqPresetRepository
) {
    operator fun invoke(query: String): List<EqPreset> {
        return repository.searchPresets(query)
    }
}
