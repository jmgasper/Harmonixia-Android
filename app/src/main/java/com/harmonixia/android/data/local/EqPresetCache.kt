package com.harmonixia.android.data.local

import android.content.Context
import com.harmonixia.android.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

private val vendorTypes = setOf("vendor", "manufacturer", "brand")
private val productTypes = setOf("product", "device")
private val eqTypes = setOf("eq", "preset", "equalizer", "filterset")

class EqPresetCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val databaseUrl: String = OPRA_DATABASE_URL
) {
    suspend fun downloadOpraDatabaseAsync(): Result<File> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(databaseUrl)
                    .build()
                val cacheFile = getCacheFile()
                val tempFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("OPRA download failed: ${response.code}")
                    }
                    val body = response.body ?: throw IllegalStateException("OPRA response body is empty")
                    body.byteStream().use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (!tempFile.renameTo(cacheFile)) {
                    tempFile.copyTo(cacheFile, overwrite = true)
                    tempFile.delete()
                }
                Logger.i(TAG, "Downloaded OPRA database to ${cacheFile.absolutePath}")
                cacheFile
            }
        }
    }

    fun getCacheFile(): File {
        return File(context.cacheDir, OPRA_CACHE_FILE)
    }

    fun isCacheValid(file: File = getCacheFile()): Boolean {
        if (!file.exists()) return false
        val maxAgeMillis = TimeUnit.DAYS.toMillis(OPRA_CACHE_EXPIRY_DAYS.toLong())
        return System.currentTimeMillis() - file.lastModified() < maxAgeMillis
    }

    fun parseJsonl(file: File): OpraDatabase {
        val vendors = mutableMapOf<String, String>()
        val products = mutableMapOf<String, OpraProduct>()
        val eqEntries = mutableListOf<JsonObject>()

        file.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach
                val element = runCatching { json.parseToJsonElement(trimmed) }.getOrNull()
                    ?: return@forEach
                val obj = element as? JsonObject ?: return@forEach
                val type = obj.stringOrNull("type", "entry_type")?.lowercase()
                when {
                    type != null && type in vendorTypes -> {
                        val id = obj.stringOrNull("id", "vendor_id") ?: return@forEach
                        val name = obj.stringOrNull("name", "manufacturer", "brand") ?: return@forEach
                        vendors[id] = name
                    }
                    type != null && type in productTypes -> {
                        val id = obj.stringOrNull("id", "product_id") ?: return@forEach
                        val vendorId = obj.stringOrNull("vendor_id", "manufacturer_id")
                        val model = obj.stringOrNull("model", "name")
                        val manufacturer = obj.stringOrNull("manufacturer", "brand")
                        products[id] = OpraProduct(
                            id = id,
                            vendorId = vendorId,
                            model = model,
                            manufacturer = manufacturer
                        )
                    }
                    type != null && type in eqTypes -> {
                        eqEntries.add(obj)
                    }
                    type == null && hasFilterPayload(obj) -> {
                        eqEntries.add(obj)
                    }
                }
            }
        }

        return OpraDatabase(
            vendors = vendors.toMap(),
            products = products.toMap(),
            eqEntries = eqEntries
        )
    }

    private fun hasFilterPayload(obj: JsonObject): Boolean {
        return obj.containsKey("filters") || obj.containsKey("eq") || obj.containsKey("bands") || obj.containsKey("peqs")
    }

    private fun JsonObject.stringOrNull(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        }
    }

    companion object {
        const val OPRA_DATABASE_URL = "https://raw.githubusercontent.com/opra-project/OPRA/main/opra.jsonl"
        const val OPRA_CACHE_FILE = "opra-presets.jsonl"
        const val OPRA_CACHE_EXPIRY_DAYS = 7
        private const val TAG = "EqPresetCache"
    }
}

data class OpraDatabase(
    val vendors: Map<String, String>,
    val products: Map<String, OpraProduct>,
    val eqEntries: List<JsonObject>
)

data class OpraProduct(
    val id: String,
    val vendorId: String?,
    val model: String?,
    val manufacturer: String?
)
