package com.harmonixia.android.data.repository

import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.data.local.EqPresetCache
import com.harmonixia.android.data.local.EqPresetParser
import com.harmonixia.android.domain.model.EqPreset
import com.harmonixia.android.domain.model.EqPresetDetails
import com.harmonixia.android.domain.repository.EqPresetRepository
import com.harmonixia.android.util.Logger
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class EqPresetRepositoryImpl @Inject constructor(
    private val eqPresetCache: EqPresetCache,
    private val eqPresetParser: EqPresetParser,
    private val eqDataStore: EqDataStore
) : EqPresetRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var presetsCache: List<EqPreset>? = null

    init {
        scope.launch {
            loadPresets(forceRefresh = false)
        }
    }

    override suspend fun loadPresets(forceRefresh: Boolean): Result<List<EqPreset>> {
        val cached = presetsCache
        if (cached != null && !forceRefresh) {
            return Result.success(cached)
        }

        return runCatching {
            val cacheFile = eqPresetCache.getCacheFile()
            val shouldDownload = forceRefresh || !eqPresetCache.isCacheValid(cacheFile)
            val databaseFile = if (shouldDownload) {
                runCatching { eqPresetCache.downloadOpraDatabaseAsync().getOrThrow() }
                    .getOrElse {
                        Logger.w(TAG, "OPRA download failed, falling back to cache", it)
                        cacheFile
                    }
            } else {
                cacheFile
            }

            if (!databaseFile.exists()) {
                throw IllegalStateException("OPRA cache missing")
            }

            val parsed = eqPresetCache.parseJsonl(databaseFile)
            if (!shouldDownload && parsed.eqEntries.isEmpty()) {
                throw IllegalStateException("OPRA cache is empty")
            }
            val presets = eqPresetParser.normalizeOpraDatabase(parsed)
            presetsCache = presets
            ensureSelectedPresetValid(presets)
            presets
        }
    }

    override fun searchPresets(query: String): List<EqPreset> {
        val presets = presetsCache.orEmpty()
        if (query.isBlank()) return presets.take(MAX_SEARCH_RESULTS)
        val needle = normalizeText(query)
        return presets.filter { preset ->
            val haystack = buildString {
                append(preset.name)
                append(' ')
                preset.manufacturer?.let { append(it).append(' ') }
                preset.model?.let { append(it).append(' ') }
                preset.creator?.let { append(it).append(' ') }
            }
            normalizeText(haystack).contains(needle)
        }.take(MAX_SEARCH_RESULTS)
    }

    override fun getPresetById(id: String): EqPreset? {
        return presetsCache?.firstOrNull { it.id == id }
    }

    override fun getPresetDetails(id: String): EqPresetDetails {
        val preset = getPresetById(id) ?: throw IllegalArgumentException("Preset not found")
        return eqPresetParser.buildPresetDetails(preset)
    }

    private suspend fun ensureSelectedPresetValid(presets: List<EqPreset>) {
        val settings = eqDataStore.getEqSettings().first()
        val selectedId = settings.selectedPresetId ?: return
        if (presets.none { it.id == selectedId }) {
            eqDataStore.saveEqSettings(settings.copy(selectedPresetId = null))
        }
    }

    private fun normalizeText(value: String): String {
        return value.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "")
    }

    companion object {
        private const val TAG = "EqPresetRepository"
        private const val MAX_SEARCH_RESULTS = 200
    }
}
