package com.harmonixia.android.data.repository

import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.data.local.EqPresetCache
import com.harmonixia.android.data.local.EqPresetParser
import com.harmonixia.android.data.local.OpraDatabase
import com.harmonixia.android.domain.model.EqPreset
import com.harmonixia.android.domain.model.EqSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.io.File
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetRepositoryTest {

    @Test
    fun loadPresets_usesCacheWhenValid() = runBlocking {
        val cacheFile = File.createTempFile("opra", ".jsonl")
        val cache = mockk<EqPresetCache>()
        val parser = mockk<EqPresetParser>()
        val dataStore = mockk<EqDataStore>()
        val preset = EqPreset(
            id = "preset-1",
            name = "Studio",
            displayName = "Brand X Model One",
            manufacturer = "Brand X",
            model = "Model One",
            creator = "Author"
        )

        every { cache.getCacheFile() } returns cacheFile
        every { cache.isCacheValid(cacheFile) } returns true
        every { cache.parseJsonl(cacheFile) } returns OpraDatabase(
            vendors = emptyMap(),
            products = emptyMap(),
            eqEntries = listOf(JsonObject(emptyMap()))
        )
        every { parser.normalizeOpraDatabase(any()) } returns listOf(preset)
        coEvery { dataStore.getEqSettings() } returns flowOf(EqSettings())
        coEvery { dataStore.saveEqSettings(any()) } just runs

        val repository = EqPresetRepositoryImpl(cache, parser, dataStore)

        val result = repository.loadPresets(forceRefresh = false)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        coVerify(exactly = 0) { cache.downloadOpraDatabaseAsync() }
    }

    @Test
    fun loadPresets_downloadsWhenCacheInvalid() = runBlocking {
        val cacheFile = File.createTempFile("opra", ".jsonl")
        val cache = mockk<EqPresetCache>()
        val parser = mockk<EqPresetParser>()
        val dataStore = mockk<EqDataStore>()
        val preset = EqPreset(
            id = "preset-2",
            name = "Reference",
            displayName = "Brand Y Model Two"
        )

        every { cache.getCacheFile() } returns cacheFile
        every { cache.isCacheValid(cacheFile) } returns false
        coEvery { cache.downloadOpraDatabaseAsync() } returns Result.success(cacheFile)
        every { cache.parseJsonl(cacheFile) } returns OpraDatabase(
            vendors = emptyMap(),
            products = emptyMap(),
            eqEntries = listOf(JsonObject(emptyMap()))
        )
        every { parser.normalizeOpraDatabase(any()) } returns listOf(preset)
        coEvery { dataStore.getEqSettings() } returns flowOf(EqSettings())
        coEvery { dataStore.saveEqSettings(any()) } just runs

        val repository = EqPresetRepositoryImpl(cache, parser, dataStore)

        val result = repository.loadPresets(forceRefresh = false)

        assertTrue(result.isSuccess)
        coVerify { cache.downloadOpraDatabaseAsync() }
    }

    @Test
    fun searchPresets_matchesManufacturerAndModel() = runBlocking {
        val cacheFile = File.createTempFile("opra", ".jsonl")
        val cache = mockk<EqPresetCache>()
        val parser = mockk<EqPresetParser>()
        val dataStore = mockk<EqDataStore>()
        val preset = EqPreset(
            id = "preset-3",
            name = "Reference",
            displayName = "Brand Z Alpha",
            manufacturer = "Brand Z",
            model = "Alpha"
        )

        every { cache.getCacheFile() } returns cacheFile
        every { cache.isCacheValid(cacheFile) } returns true
        every { cache.parseJsonl(cacheFile) } returns OpraDatabase(
            vendors = emptyMap(),
            products = emptyMap(),
            eqEntries = listOf(JsonObject(emptyMap()))
        )
        every { parser.normalizeOpraDatabase(any()) } returns listOf(preset)
        coEvery { dataStore.getEqSettings() } returns flowOf(EqSettings())
        coEvery { dataStore.saveEqSettings(any()) } just runs

        val repository = EqPresetRepositoryImpl(cache, parser, dataStore)
        repository.loadPresets(forceRefresh = false)

        val results = repository.searchPresets("brand z")

        assertEquals(1, results.size)
        assertNotNull(repository.getPresetById("preset-3"))
    }
}
