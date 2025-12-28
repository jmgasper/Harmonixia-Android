package com.harmonixia.android.domain.usecase

import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectToServerUseCaseTest {

    private val repository = mockk<MusicAssistantRepository>()
    private val settingsDataStore = mockk<SettingsDataStore>()
    private val useCase = ConnectToServerUseCase(repository, settingsDataStore)

    @Test
    fun invoke_success_savesSettings() = runBlocking {
        coEvery { repository.connect(any(), any()) } returns Result.success(Unit)
        coEvery { settingsDataStore.saveServerUrl(any()) } just runs
        coEvery { settingsDataStore.saveAuthToken(any()) } just runs

        val result = useCase("https://example.com", "token-123")

        assertTrue(result.isSuccess)
        coVerify { repository.connect("https://example.com", "token-123") }
        coVerify { settingsDataStore.saveServerUrl("https://example.com") }
        coVerify { settingsDataStore.saveAuthToken("token-123") }
    }

    @Test
    fun invoke_normalizesUrl() = runBlocking {
        coEvery { repository.connect(any(), any()) } returns Result.success(Unit)
        coEvery { settingsDataStore.saveServerUrl(any()) } just runs
        coEvery { settingsDataStore.saveAuthToken(any()) } just runs

        val result = useCase("example.com/", "token-123")

        assertTrue(result.isSuccess)
        coVerify { repository.connect("http://example.com", "token-123") }
        coVerify { settingsDataStore.saveServerUrl("http://example.com") }
    }

    @Test
    fun invoke_emptyUrl_returnsFailure() = runBlocking {
        val result = useCase("", "token-123")

        assertTrue(result.isFailure)
        assertEquals(
            "Server URL cannot be empty",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.connect(any(), any()) }
    }

    @Test
    fun invoke_emptyToken_allowsConnection() = runBlocking {
        coEvery { repository.connect(any(), any()) } returns Result.success(Unit)
        coEvery { settingsDataStore.saveServerUrl(any()) } just runs
        coEvery { settingsDataStore.saveAuthToken(any()) } just runs

        val result = useCase("example.com", "")

        assertTrue(result.isSuccess)
        coVerify { repository.connect("http://example.com", "") }
        coVerify { settingsDataStore.saveAuthToken("") }
    }

    @Test
    fun invoke_withoutPersist_doesNotSaveSettings() = runBlocking {
        coEvery { repository.connect(any(), any()) } returns Result.success(Unit)

        val result = useCase("https://example.com", "token-123", persistSettings = false)

        assertTrue(result.isSuccess)
        coVerify { repository.connect("https://example.com", "token-123") }
        coVerify(exactly = 0) { settingsDataStore.saveServerUrl(any()) }
        coVerify(exactly = 0) { settingsDataStore.saveAuthToken(any()) }
    }
}
