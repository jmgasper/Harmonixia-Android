package com.harmonixia.android.data.local

import android.content.Context
import com.harmonixia.android.R
import io.mockk.every
import io.mockk.mockk
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetCacheIntegrationTest {

    @Test
    fun downloadAndParseDatabase() {
        runBlocking {
            val server = MockWebServer()
            val jsonl = """
                {"type":"vendor","id":"brand-x","name":"Brand X"}
                {"type":"product","id":"prod-1","vendor_id":"brand-x","model":"Model One"}
                {"type":"eq","id":"preset-1","product_id":"prod-1","filters":[{"frequency":1000,"gain":3,"q":1.2}]}
            """.trimIndent()
            server.enqueue(MockResponse().setResponseCode(200).setBody(jsonl))
            server.start()

            val cacheDir = createTempDirectory(prefix = "eqpreset-cache-test-").toFile()
            val context = mockk<Context>(relaxed = true)
            every { context.cacheDir } returns cacheDir

            val cache = EqPresetCache(
                context = context,
                okHttpClient = OkHttpClient(),
                json = Json { ignoreUnknownKeys = true },
                databaseUrl = server.url("/opra.jsonl").toString()
            )

            val result = cache.downloadOpraDatabaseAsync()

            assertTrue("Download failed: ${result.exceptionOrNull()}", result.isSuccess)
            val file = result.getOrThrow()
            assertTrue(file.exists())

            val parsed = cache.parseJsonl(file)
            assertEquals(1, parsed.eqEntries.size)

            server.shutdown()
            cacheDir.deleteRecursively()
        }
    }

    @Test
    fun downloadDatabase_emptyResponseBody_returnsFailure() {
        runBlocking {
            val server = MockWebServer()
            server.enqueue(MockResponse().setResponseCode(200).setBody(""))
            server.start()

            val cacheDir = createTempDirectory(prefix = "eqpreset-cache-empty-test-").toFile()
            val context = mockk<Context>(relaxed = true)
            every { context.cacheDir } returns cacheDir
            every { context.getString(R.string.eq_validation_opra_response_body_empty) } returns
                "OPRA response body is empty"
            every { context.getString(R.string.eq_validation_opra_download_failed_http, any()) } answers {
                "OPRA download failed (HTTP ${secondArg<Int>()})"
            }

            val cache = EqPresetCache(
                context = context,
                okHttpClient = OkHttpClient(),
                json = Json { ignoreUnknownKeys = true },
                databaseUrl = server.url("/opra-empty.jsonl").toString()
            )

            val result = cache.downloadOpraDatabaseAsync()

            assertTrue("Expected download failure for empty response body", result.isFailure)
            val message = result.exceptionOrNull()?.message.orEmpty()
            assertTrue(
                "Expected empty-body error message, got: $message",
                message.contains("response body is empty", ignoreCase = true)
            )
            assertTrue(
                "Expected no cache file on failed download",
                !cache.getCacheFile().exists()
            )

            server.shutdown()
            cacheDir.deleteRecursively()
        }
    }
}
