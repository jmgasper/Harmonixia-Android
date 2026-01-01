package com.harmonixia.android.domain.usecase

import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.domain.model.AuthMethod
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
        coEvery { settingsDataStore.saveAuthMethod(any()) } just runs
        coEvery { settingsDataStore.saveUsername(any()) } just runs
        coEvery { settingsDataStore.savePassword(any()) } just runs

        val result = useCase(
            serverUrl = "https://example.com",
            authToken = "token-123",
            authMethod = AuthMethod.TOKEN
        )

        assertTrue(result.isSuccess)
        coVerify { repository.connect("https://example.com", "token-123") }
        coVerify { settingsDataStore.saveServerUrl("https://example.com") }
        coVerify { settingsDataStore.saveAuthToken("token-123") }
        coVerify { settingsDataStore.saveAuthMethod(AuthMethod.TOKEN) }
        coVerify { settingsDataStore.saveUsername("") }
        coVerify { settingsDataStore.savePassword("") }
    }

    @Test
    fun invoke_normalizesUrl() = runBlocking {
        coEvery { repository.connect(any(), any()) } returns Result.success(Unit)
        coEvery { settingsDataStore.saveServerUrl(any()) } just runs
        coEvery { settingsDataStore.saveAuthToken(any()) } just runs
        coEvery { settingsDataStore.saveAuthMethod(any()) } just runs
        coEvery { settingsDataStore.saveUsername(any()) } just runs
        coEvery { settingsDataStore.savePassword(any()) } just runs

        val result = useCase(
            serverUrl = "example.com/",
            authToken = "token-123",
            authMethod = AuthMethod.TOKEN
        )

        assertTrue(result.isSuccess)
        coVerify { repository.connect("http://example.com", "token-123") }
        coVerify { settingsDataStore.saveServerUrl("http://example.com") }
    }

    @Test
    fun invoke_emptyUrl_returnsFailure() = runBlocking {
        val result = useCase(
            serverUrl = "",
            authToken = "token-123",
            authMethod = AuthMethod.TOKEN
        )

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
        coEvery { settingsDataStore.saveAuthMethod(any()) } just runs
        coEvery { settingsDataStore.saveUsername(any()) } just runs
        coEvery { settingsDataStore.savePassword(any()) } just runs

        val result = useCase(
            serverUrl = "example.com",
            authToken = "",
            authMethod = AuthMethod.TOKEN
        )

        assertTrue(result.isSuccess)
        coVerify { repository.connect("http://example.com", "") }
        coVerify { settingsDataStore.saveAuthToken("") }
        coVerify { settingsDataStore.saveAuthMethod(AuthMethod.TOKEN) }
    }

    @Test
    fun invoke_withoutPersist_doesNotSaveSettings() = runBlocking {
        coEvery { repository.connect(any(), any()) } returns Result.success(Unit)

        val result = useCase(
            serverUrl = "https://example.com",
            authToken = "token-123",
            authMethod = AuthMethod.TOKEN,
            persistSettings = false
        )

        assertTrue(result.isSuccess)
        coVerify { repository.connect("https://example.com", "token-123") }
        coVerify(exactly = 0) { settingsDataStore.saveServerUrl(any()) }
        coVerify(exactly = 0) { settingsDataStore.saveAuthToken(any()) }
        coVerify(exactly = 0) { settingsDataStore.saveAuthMethod(any()) }
        coVerify(exactly = 0) { settingsDataStore.saveUsername(any()) }
        coVerify(exactly = 0) { settingsDataStore.savePassword(any()) }
    }

    @Test
    fun invoke_usernamePassword_success_savesSettings() = runBlocking {
        coEvery { repository.loginWithCredentials(any(), any(), any()) } returns Result.success("token-456")
        coEvery { repository.connect(any(), any()) } returns Result.success(Unit)
        coEvery { settingsDataStore.saveServerUrl(any()) } just runs
        coEvery { settingsDataStore.saveAuthToken(any()) } just runs
        coEvery { settingsDataStore.saveAuthMethod(any()) } just runs
        coEvery { settingsDataStore.saveUsername(any()) } just runs
        coEvery { settingsDataStore.savePassword(any()) } just runs

        val result = useCase(
            serverUrl = "https://example.com",
            authMethod = AuthMethod.USERNAME_PASSWORD,
            username = "user",
            password = "pass"
        )

        assertTrue(result.isSuccess)
        coVerify { repository.loginWithCredentials("https://example.com", "user", "pass") }
        coVerify { repository.connect("https://example.com", "token-456") }
        coVerify { settingsDataStore.saveServerUrl("https://example.com") }
        coVerify { settingsDataStore.saveAuthToken("token-456") }
        coVerify { settingsDataStore.saveAuthMethod(AuthMethod.USERNAME_PASSWORD) }
        coVerify { settingsDataStore.saveUsername("user") }
        coVerify { settingsDataStore.savePassword("pass") }
    }

    @Test
    fun invoke_usernamePassword_emptyUsername_returnsFailure() = runBlocking {
        val result = useCase(
            serverUrl = "https://example.com",
            authMethod = AuthMethod.USERNAME_PASSWORD,
            username = "",
            password = "pass"
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Username cannot be empty",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.loginWithCredentials(any(), any(), any()) }
        coVerify(exactly = 0) { repository.connect(any(), any()) }
    }

    @Test
    fun invoke_usernamePassword_emptyPassword_returnsFailure() = runBlocking {
        val result = useCase(
            serverUrl = "https://example.com",
            authMethod = AuthMethod.USERNAME_PASSWORD,
            username = "user",
            password = ""
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Password cannot be empty",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.loginWithCredentials(any(), any(), any()) }
        coVerify(exactly = 0) { repository.connect(any(), any()) }
    }

    @Test
    fun invoke_usernamePassword_loginFailure_propagates() = runBlocking {
        val error = SecurityException("Invalid credentials")
        coEvery { repository.loginWithCredentials(any(), any(), any()) } returns Result.failure(error)

        val result = useCase(
            serverUrl = "https://example.com",
            authMethod = AuthMethod.USERNAME_PASSWORD,
            username = "user",
            password = "pass"
        )

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        coVerify { repository.loginWithCredentials("https://example.com", "user", "pass") }
        coVerify(exactly = 0) { repository.connect(any(), any()) }
    }
}
