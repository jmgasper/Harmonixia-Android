package com.harmonixia.android.domain.usecase

import android.content.Context
import com.harmonixia.android.R
import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.data.local.EqPresetParser
import com.harmonixia.android.domain.model.EqBandConfig
import com.harmonixia.android.domain.model.EqPreset
import com.harmonixia.android.domain.model.EqSettings
import com.harmonixia.android.domain.repository.EqPresetRepository
import com.harmonixia.android.service.playback.EqualizerManager
import com.harmonixia.android.service.playback.PlaybackServiceConnection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyEqPresetUseCaseTest {

    @Test
    fun invoke_appliesPresetAndSavesSettings() = runBlocking {
        val context = mockk<Context>(relaxed = true)
        val repository = mockk<EqPresetRepository>()
        val dataStore = mockk<EqDataStore>()
        val parser = mockk<EqPresetParser>()
        val equalizerManager = mockk<EqualizerManager>()
        val playbackServiceConnection = mockk<PlaybackServiceConnection>()

        val preset = EqPreset(id = "preset-1", name = "Test", displayName = "Test")
        val bands = listOf(EqBandConfig(freq = 1000.0, bandwidth = 200.0, gain = 3.0))

        every { playbackServiceConnection.connect() } just runs
        coEvery { repository.getPresetById("preset-1") } returns preset
        coEvery { repository.loadPresets(any()) } returns Result.success(listOf(preset))
        every { context.getString(R.string.eq_validation_preset_not_found) } returns "Preset not found"
        every { parser.convertToAndroidBands(preset) } returns bands
        every { equalizerManager.applyPreset(bands) } just runs
        every { equalizerManager.setSoftwareEqFilters(any()) } just runs
        every { equalizerManager.setEnabled(true) } just runs
        every { dataStore.getEqSettings() } returns flowOf(EqSettings(enabled = true))
        coEvery { dataStore.saveEqSettings(any()) } just runs

        val useCase = ApplyEqPresetUseCase(
            context,
            repository,
            dataStore,
            parser,
            equalizerManager,
            playbackServiceConnection
        )

        val result = useCase("preset-1")

        assertTrue(result.isSuccess)
        verify { playbackServiceConnection.connect() }
        verify { equalizerManager.applyPreset(bands) }
        verify { equalizerManager.setSoftwareEqFilters(preset.filters) }
        verify { equalizerManager.setEnabled(true) }
        coVerify { dataStore.saveEqSettings(match { it.selectedPresetId == "preset-1" }) }
    }

    @Test
    fun invoke_missingPreset_returnsFailure() = runBlocking {
        val context = mockk<Context>(relaxed = true)
        val repository = mockk<EqPresetRepository>()
        val dataStore = mockk<EqDataStore>()
        val parser = mockk<EqPresetParser>()
        val equalizerManager = mockk<EqualizerManager>()
        val playbackServiceConnection = mockk<PlaybackServiceConnection>()

        every { playbackServiceConnection.connect() } just runs
        coEvery { repository.getPresetById("missing") } returns null
        coEvery { repository.loadPresets(any()) } returns Result.success(emptyList())
        every { context.getString(R.string.eq_validation_preset_not_found) } returns "Preset not found"

        val useCase = ApplyEqPresetUseCase(
            context,
            repository,
            dataStore,
            parser,
            equalizerManager,
            playbackServiceConnection
        )

        val result = useCase("missing")

        assertTrue(result.isFailure)
    }
}
