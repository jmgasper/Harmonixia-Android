package com.harmonixia.android.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class NetworkConnectivityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
        as ConnectivityManager

    private val _networkAvailabilityFlow = MutableStateFlow(isNetworkAvailable())
    val networkAvailabilityFlow: StateFlow<Boolean> = _networkAvailabilityFlow.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkAvailability()
        }

        override fun onLost(network: Network) {
            updateNetworkAvailability()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateNetworkAvailability()
        }
    }

    init {
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }.onFailure { error ->
            Logger.w(TAG, "Failed to register network callback", error)
        }
        updateNetworkAvailability()
    }

    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isOfflineMode(): Boolean {
        return !isNetworkAvailable() || isAirplaneModeOn()
    }

    fun refresh() {
        updateNetworkAvailability()
    }

    private fun isAirplaneModeOn(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) == 1
    }

    private fun updateNetworkAvailability() {
        val available = isNetworkAvailable()
        if (available == _networkAvailabilityFlow.value) return
        _networkAvailabilityFlow.value = available
        Logger.i(TAG, "Network available: $available, airplane mode: ${isAirplaneModeOn()}")
    }

    private companion object {
        private const val TAG = "NetworkConnectivityManager"
    }
}
