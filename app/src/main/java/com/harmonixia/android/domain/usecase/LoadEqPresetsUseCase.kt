package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.EqPreset
import com.harmonixia.android.domain.repository.EqPresetRepository

class LoadEqPresetsUseCase(
    private val repository: EqPresetRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<EqPreset>> {
        return repository.loadPresets(forceRefresh)
    }
}
