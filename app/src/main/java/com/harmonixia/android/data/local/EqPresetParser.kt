package com.harmonixia.android.data.local

import com.harmonixia.android.domain.model.EqBandConfig
import com.harmonixia.android.domain.model.EqFilter
import com.harmonixia.android.domain.model.EqPreset
import com.harmonixia.android.domain.model.EqPresetDetails
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

class EqPresetParser @Inject constructor() {
    fun normalizeOpraDatabase(database: OpraDatabase): List<EqPreset> {
        return database.eqEntries.mapNotNull { entry ->
            normalizeOpraDatabaseEntry(entry, database.vendors, database.products)
        }
    }

    fun normalizeOpraDatabaseEntry(
        entry: JsonObject,
        vendors: Map<String, String>,
        products: Map<String, OpraProduct>
    ): EqPreset? {
        val data = entry["data"] as? JsonObject
        fun stringOrNull(vararg keys: String): String? {
            return data?.stringOrNull(*keys) ?: entry.stringOrNull(*keys)
        }

        val name = stringOrNull("name", "preset_name", "title") ?: "EQ Preset"
        val description = stringOrNull("description", "notes", "details")
        val creator = stringOrNull("creator", "author", "by")
        val productId = stringOrNull("product_id", "device_id")
        val vendorId = stringOrNull("vendor_id", "manufacturer_id")
        val product = productId?.let { products[it] }

        val manufacturer = stringOrNull("manufacturer", "brand")
            ?: product?.manufacturer
            ?: vendorId?.let { vendors[it] }
            ?: product?.vendorId?.let { vendors[it] }
        val model = stringOrNull("model", "device") ?: product?.model

        val filters = extractFilterElements(entry).mapNotNull { filterElement ->
            when (filterElement) {
                is JsonObject -> normalizeFilter(filterElement)
                is JsonArray -> normalizeFilterArray(filterElement)
                else -> null
            }
        }

        if (filters.isEmpty()) return null

        val presetId = stringOrNull("id", "preset_id")
            ?: generatePresetId(name, manufacturer, model, creator)
        val displayName = buildDisplayName(manufacturer, model, creator, name)

        return EqPreset(
            id = presetId,
            name = name,
            displayName = displayName,
            manufacturer = manufacturer,
            model = model,
            creator = creator,
            description = description,
            filters = filters
        )
    }

    fun normalizeFilter(filter: JsonObject): EqFilter? {
        val typeRaw = filter.stringOrNull("type", "filter_type")
        val type = typeRaw?.lowercase(Locale.US) ?: DEFAULT_FILTER_TYPE

        val frequency = filter.doubleOrNull("frequency", "freq", "f0", "fc") ?: return null
        if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY) return null

        val gain = filter.doubleOrNull("gain", "gain_db", "gainDb", "db", "g") ?: return null
        if (gain < MIN_GAIN_DB || gain > MAX_GAIN_DB) return null

        val qValue = filter.doubleOrNull("q", "Q", "quality", "q_factor")
        val bandwidth = filter.doubleOrNull("bandwidth", "bw")
        val computedQ = when {
            qValue != null && qValue > 0.0 -> qValue
            bandwidth != null && bandwidth > 0.0 -> frequency / bandwidth
            type.contains("shelf") -> DEFAULT_SHELF_Q
            else -> DEFAULT_Q
        }

        val normalizedType = if (type.contains("shelf")) DEFAULT_FILTER_TYPE else type

        return EqFilter(
            frequency = frequency,
            gain = gain,
            q = computedQ,
            type = normalizedType
        )
    }

    fun buildDisplayName(
        manufacturer: String?,
        model: String?,
        creator: String?,
        fallbackName: String
    ): String {
        val parts = listOfNotNull(
            manufacturer?.takeIf { it.isNotBlank() },
            model?.takeIf { it.isNotBlank() }
        )
        val base = if (parts.isEmpty()) fallbackName else parts.joinToString(" ")
        return if (creator.isNullOrBlank()) {
            base
        } else if (base.isNotBlank()) {
            "$base - $creator"
        } else {
            creator
        }
    }

    fun convertToAndroidBands(preset: EqPreset): List<EqBandConfig> {
        return preset.filters.mapNotNull { filter ->
            val bandwidth = if (filter.q > 0.0) filter.frequency / filter.q else filter.frequency
            val clampedBandwidth = max(MIN_BANDWIDTH, bandwidth)
            EqBandConfig(
                freq = filter.frequency.coerceIn(MIN_FREQUENCY, MAX_FREQUENCY),
                bandwidth = clampedBandwidth,
                gain = filter.gain.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
            )
        }
    }

    fun buildPresetDetails(preset: EqPreset): EqPresetDetails {
        val supportedBands = preset.filters.size
        val droppedFilters = 0
        return EqPresetDetails(
            presetId = preset.id,
            filterCount = preset.filters.size,
            supportedBands = supportedBands,
            droppedFilters = droppedFilters
        )
    }

    private fun extractFilterElements(entry: JsonObject): List<JsonElement> {
        val data = entry["data"] as? JsonObject
        val parameters = data?.get("parameters") as? JsonObject
        val containers = listOfNotNull(entry, data, parameters)
        for (container in containers) {
            val elements = extractFilterElementsFrom(container)
            if (elements.isNotEmpty()) return elements
        }
        return emptyList()
    }

    private fun extractFilterElementsFrom(container: JsonObject): List<JsonElement> {
        val candidates = listOf("filters", "eq", "eqs", "bands", "peqs", "filters_biquad")
        for (key in candidates) {
            val element = container[key] ?: continue
            when (element) {
                is JsonArray -> return element
                is JsonObject -> {
                    val nested = element["filters"]
                    if (nested is JsonArray) return nested
                }
                else -> Unit
            }
        }
        return emptyList()
    }

    private fun normalizeFilterArray(array: JsonArray): EqFilter? {
        if (array.size < 3) return null
        val frequency = array[0].jsonPrimitive.doubleOrNull ?: return null
        val gain = array[1].jsonPrimitive.doubleOrNull ?: return null
        val q = array[2].jsonPrimitive.doubleOrNull ?: DEFAULT_Q
        if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY) return null
        if (gain < MIN_GAIN_DB || gain > MAX_GAIN_DB) return null
        return EqFilter(
            frequency = frequency,
            gain = gain,
            q = q,
            type = DEFAULT_FILTER_TYPE
        )
    }

    private fun generatePresetId(
        name: String,
        manufacturer: String?,
        model: String?,
        creator: String?
    ): String {
        val seed = listOf(name, manufacturer, model, creator)
            .filterNot { it.isNullOrBlank() }
            .joinToString("|")
        return UUID.nameUUIDFromBytes(seed.toByteArray()).toString()
    }

    private fun JsonObject.stringOrNull(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        }
    }

    private fun JsonObject.doubleOrNull(vararg keys: String): Double? {
        return keys.firstNotNullOfOrNull { key ->
            val element = this[key] ?: return@firstNotNullOfOrNull null
            element.jsonPrimitive.doubleOrNull
                ?: element.jsonPrimitive.contentOrNull?.toDoubleOrNull()
        }
    }

    companion object {
        private const val MIN_FREQUENCY = 20.0
        private const val MAX_FREQUENCY = 20000.0
        private const val MIN_GAIN_DB = -24.0
        private const val MAX_GAIN_DB = 12.0
        private const val DEFAULT_Q = 1.0
        private const val DEFAULT_SHELF_Q = 0.7
        private const val DEFAULT_FILTER_TYPE = "peaking"
        private const val MIN_BANDWIDTH = 1.0
    }
}
