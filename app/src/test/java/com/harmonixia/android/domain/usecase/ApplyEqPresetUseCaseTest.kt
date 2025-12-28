package com.harmonixia.android.domain.usecase

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
        every { parser.convertToAndroidBands(preset) } returns bands
        every { equalizerManager.applyPreset(bands) } just runs
        every { equalizerManager.setEnabled(true) } just runs
        every { dataStore.getEqSettings() } returns flowOf(EqSettings(enabled = true))
        coEvery { dataStore.saveEqSettings(any()) } just runs

        val useCase = ApplyEqPresetUseCase(
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
        verify { equalizerManager.setEnabled(true) }
        coVerify { dataStore.saveEqSettings(match { it.selectedPresetId == "preset-1" }) }
    }

    @Test
    fun invoke_missingPreset_returnsFailure() = runBlocking {
        val repository = mockk<EqPresetRepository>()
        val dataStore = mockk<EqDataStore>()
        val parser = mockk<EqPresetParser>()
        val equalizerManager = mockk<EqualizerManager>()
        val playbackServiceConnection = mockk<PlaybackServiceConnection>()

        every { playbackServiceConnection.connect() } just runs
        coEvery { repository.getPresetById("missing") } returns null
        coEvery { repository.loadPresets(any()) } returns Result.success(emptyList())

        val useCase = ApplyEqPresetUseCase(
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
