package com.harmonixia.android.util

import android.content.Context
import com.harmonixia.android.R
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.AuthMethod
import com.harmonixia.android.domain.usecase.ConnectToServerUseCase
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ConnectionRecoveryManagerTest {

    private val context = mockk<Context>(relaxed = true).apply {
        every { getString(R.string.error_invalid_url) } returns ERROR_INVALID_URL
    }
    private val settingsDataStore = mockk<SettingsDataStore>()
    private val connectToServerUseCase = mockk<ConnectToServerUseCase>()
    private val getConnectionStateUseCase = mockk<GetConnectionStateUseCase>()
    private val networkConnectivityManager = mockk<NetworkConnectivityManager>()
    private val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private var nowMs = 1_000L

    private lateinit var manager: ConnectionRecoveryManager

    @Before
    fun setUp() {
        every { getConnectionStateUseCase.invoke() } returns connectionState
        every { networkConnectivityManager.isOfflineMode() } returns false
        every { settingsDataStore.getAuthMethod() } returns flowOf(AuthMethod.TOKEN)
        every { settingsDataStore.getUsername() } returns flowOf("")
        every { settingsDataStore.getPassword() } returns flowOf("")
        every { settingsDataStore.getAuthToken() } returns flowOf("token-123")

        manager = ConnectionRecoveryManager(
            context = context,
            settingsDataStore = settingsDataStore,
            connectToServerUseCase = connectToServerUseCase,
            getConnectionStateUseCase = getConnectionStateUseCase,
            networkConnectivityManager = networkConnectivityManager
        )
        manager.elapsedRealtimeMsProvider = { nowMs }
    }

    @Test
    fun maybeReconnect_invalidPersistedUrl_skipsRepeatedReconnectAttempts() = runBlocking {
        val invalidUrl = "not-a-valid-url]"
        every { settingsDataStore.getServerUrl() } returns flowOf(invalidUrl)
        coEvery { connectToServerUseCase(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(IllegalArgumentException(ERROR_INVALID_URL))

        manager.maybeReconnectForTest("first")
        manager.maybeReconnectForTest("second")

        coVerify(exactly = 1) {
            connectToServerUseCase(
                invalidUrl,
                "token-123",
                AuthMethod.TOKEN,
                "",
                "",
                false
            )
        }
    }

    @Test
    fun maybeReconnect_whenServerUrlUpdatedFromInvalidToValid_attemptsReconnectAgain() = runBlocking {
        var serverUrl = "not-a-valid-url]"
        every { settingsDataStore.getServerUrl() } answers { flowOf(serverUrl) }
        coEvery { connectToServerUseCase(any(), any(), any(), any(), any(), any()) } answers {
            val url = args[0] as String
            if (url == "https://example.com") {
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException(ERROR_INVALID_URL))
            }
        }

        manager.maybeReconnectForTest("invalid")
        serverUrl = "https://example.com"
        manager.maybeReconnectForTest("updated")

        coVerify(exactly = 1) {
            connectToServerUseCase(
                "https://example.com",
                "token-123",
                AuthMethod.TOKEN,
                "",
                "",
                false
            )
        }
        coVerify(exactly = 2) {
            connectToServerUseCase(any(), any(), any(), any(), any(), false)
        }
    }

    private companion object {
        private const val ERROR_INVALID_URL = "Server URL is invalid"
    }
}
