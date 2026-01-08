package com.harmonixia.android.util

import android.os.SystemClock
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.model.AuthMethod
import com.harmonixia.android.domain.usecase.ConnectToServerUseCase
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class ConnectionRecoveryManager @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val connectToServerUseCase: ConnectToServerUseCase,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectionState: StateFlow<ConnectionState> = getConnectionStateUseCase()
    private val reconnectInFlight = AtomicBoolean(false)
    @Volatile private var lastReconnectAttemptAtMs = 0L
    private var networkMonitorJob: Job? = null

    fun start() {
        if (networkMonitorJob != null) return
        networkMonitorJob = scope.launch {
            networkConnectivityManager.networkAvailabilityFlow.collect { available ->
                if (available) {
                    requestReconnect(REASON_NETWORK_AVAILABLE)
                }
            }
        }
    }

    fun onAppResumed() {
        requestReconnect(REASON_APP_RESUMED)
    }

    fun requestReconnect(reason: String) {
        scope.launch {
            maybeReconnect(reason)
        }
    }

    private suspend fun maybeReconnect(reason: String) {
        if (networkConnectivityManager.isOfflineMode()) return
        val state = connectionState.value
        if (state is ConnectionState.Connected || state is ConnectionState.Connecting) return
        val now = SystemClock.elapsedRealtime()
        if (lastReconnectAttemptAtMs > 0L &&
            now - lastReconnectAttemptAtMs < MIN_RECONNECT_INTERVAL_MS
        ) {
            return
        }
        if (!reconnectInFlight.compareAndSet(false, true)) return
        try {
            val serverUrl = settingsDataStore.getServerUrl().first().trim()
            if (serverUrl.isBlank()) return
            val authMethod = settingsDataStore.getAuthMethod().first()
            val username = settingsDataStore.getUsername().first().trim()
            val password = settingsDataStore.getPassword().first()
            if (authMethod == AuthMethod.USERNAME_PASSWORD &&
                (username.isBlank() || password.isBlank())
            ) {
                return
            }
            lastReconnectAttemptAtMs = SystemClock.elapsedRealtime()
            val token = settingsDataStore.getAuthToken().first().trim()
            Logger.i(TAG, "Attempting reconnect ($reason) to ${Logger.sanitizeUrl(serverUrl)}")
            connectToServerUseCase(
                serverUrl,
                token,
                authMethod,
                username,
                password,
                persistSettings = false
            ).onFailure { error ->
                Logger.w(TAG, "Reconnect failed: ${error.message}", error)
            }
        } finally {
            reconnectInFlight.set(false)
        }
    }

    private companion object {
        private const val TAG = "ConnectionRecovery"
        private const val MIN_RECONNECT_INTERVAL_MS = 3_000L
        private const val REASON_APP_RESUMED = "app_resumed"
        private const val REASON_NETWORK_AVAILABLE = "network_available"
    }
}
