package com.harmonixia.android.domain.repository

import com.harmonixia.android.domain.model.EqPreset
import com.harmonixia.android.domain.model.EqPresetDetails

interface EqPresetRepository {
    suspend fun loadPresets(forceRefresh: Boolean = false): Result<List<EqPreset>>
    fun searchPresets(query: String): List<EqPreset>
    fun getPresetById(id: String): EqPreset?
    fun getPresetDetails(id: String): EqPresetDetails
}
