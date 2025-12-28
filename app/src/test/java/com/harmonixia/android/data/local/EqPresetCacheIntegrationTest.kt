package com.harmonixia.android.data.local

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.File
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

            val cacheDir = createTempDir()
            val context = mockk<Context>()
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
}
