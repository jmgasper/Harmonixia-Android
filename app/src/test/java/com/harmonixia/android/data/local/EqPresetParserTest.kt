package com.harmonixia.android.data.local

import android.content.Context
import com.harmonixia.android.domain.model.EqFilter
import com.harmonixia.android.domain.model.EqPreset
import io.mockk.mockk
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetParserTest {

    private val parser = EqPresetParser()

    @Test
    fun parseJsonl_readsVendorProductAndEqEntries() {
        val context = mockk<Context>(relaxed = true)
        val json = Json { ignoreUnknownKeys = true }
        val cache = EqPresetCache(context, OkHttpClient(), json)
        val file = File.createTempFile("opra", ".jsonl")
        file.writeText(
            """
            {"type":"vendor","id":"brand-x","name":"Brand X"}
            {"type":"product","id":"prod-1","vendor_id":"brand-x","model":"Model One"}
            {"type":"eq","id":"preset-1","product_id":"prod-1","filters":[{"frequency":1000,"gain":3,"q":1.2}]}
            """.trimIndent()
        )

        val database = cache.parseJsonl(file)

        assertEquals("Brand X", database.vendors["brand-x"])
        assertEquals("Model One", database.products["prod-1"]?.model)
        assertEquals(1, database.eqEntries.size)
    }

    @Test
    fun normalizeOpraDatabaseEntry_buildsDisplayNameFromProduct() {
        val entry = buildJsonObject {
            put("name", JsonPrimitive("Target"))
            put("product_id", JsonPrimitive("prod-1"))
            put(
                "filters",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("frequency", JsonPrimitive(1000))
                            put("gain", JsonPrimitive(3.0))
                            put("q", JsonPrimitive(1.2))
                        }
                    )
                }
            )
        }
        val vendors = mapOf("vendor-1" to "Brand X")
        val products = mapOf(
            "prod-1" to OpraProduct(
                id = "prod-1",
                vendorId = "vendor-1",
                model = "Model One",
                manufacturer = null
            )
        )

        val preset = parser.normalizeOpraDatabaseEntry(entry, vendors, products)

        assertNotNull(preset)
        assertEquals("Brand X Model One", preset?.displayName)
    }

    @Test
    fun normalizeFilter_shelfDefaultsToPeaking() {
        val filter = buildJsonObject {
            put("frequency", JsonPrimitive(100))
            put("gain", JsonPrimitive(4.0))
            put("type", JsonPrimitive("low_shelf"))
        }

        val normalized = parser.normalizeFilter(filter)

        assertNotNull(normalized)
        assertEquals("peaking", normalized?.type)
        assertEquals(0.7, normalized?.q ?: 0.0, 0.001)
    }

    @Test
    fun convertToAndroidBands_clampsGainAndBandwidth() {
        val preset = EqPreset(
            id = "preset",
            name = "Test",
            displayName = "Test",
            filters = listOf(
                EqFilter(frequency = 1000.0, gain = 18.0, q = 0.5, type = "peaking")
            )
        )

        val bands = parser.convertToAndroidBands(preset)

        assertEquals(1, bands.size)
        assertTrue(bands.first().gain <= 12.0)
        assertTrue(bands.first().bandwidth >= 1.0)
    }
}
