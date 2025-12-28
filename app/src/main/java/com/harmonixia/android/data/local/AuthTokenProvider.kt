package com.harmonixia.android.data.local

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Singleton
class AuthTokenProvider @Inject constructor(
    settingsDataStore: SettingsDataStore
) {
    @Volatile
    private var authToken: String = ""

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            settingsDataStore.getAuthToken().collect { token ->
                authToken = token
            }
        }
    }

    fun getAuthToken(): String = authToken
}
