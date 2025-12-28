package com.harmonixia.android.domain.usecase

import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.domain.model.EqSettings
import kotlinx.coroutines.flow.Flow

class GetEqSettingsUseCase(
    private val eqDataStore: EqDataStore
) {
    operator fun invoke(): Flow<EqSettings> {
        return eqDataStore.getEqSettings()
    }
}
