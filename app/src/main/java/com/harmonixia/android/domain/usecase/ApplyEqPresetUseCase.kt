package com.harmonixia.android.domain.usecase

import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.data.local.EqPresetParser
import com.harmonixia.android.domain.repository.EqPresetRepository
import com.harmonixia.android.service.playback.EqualizerManager
import com.harmonixia.android.service.playback.PlaybackServiceConnection
import kotlinx.coroutines.flow.first

class ApplyEqPresetUseCase(
    private val repository: EqPresetRepository,
    private val eqDataStore: EqDataStore,
    private val eqPresetParser: EqPresetParser,
    private val equalizerManager: EqualizerManager,
    private val playbackServiceConnection: PlaybackServiceConnection
) {
    suspend operator fun invoke(presetId: String): Result<Unit> {
        return runCatching {
            val preset = repository.getPresetById(presetId)
                ?: run {
                    repository.loadPresets(forceRefresh = false).getOrThrow()
                    repository.getPresetById(presetId)
                }
                ?: throw IllegalStateException("Preset not found")

            playbackServiceConnection.connect()

            val bands = eqPresetParser.convertToAndroidBands(preset)
            equalizerManager.applyPreset(bands)
            equalizerManager.setSoftwareEqFilters(preset.filters)
            val currentSettings = eqDataStore.getEqSettings().first()
            equalizerManager.setEnabled(currentSettings.enabled)
            eqDataStore.saveEqSettings(
                currentSettings.copy(
                    selectedPresetId = presetId,
                    customBands = null
                )
            )
        }
    }
}
