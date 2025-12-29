package com.harmonixia.android

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.manager.DownloadManager
import com.harmonixia.android.domain.usecase.ConnectToServerUseCase
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.util.Logger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class HarmonixiaApplication : Application() {
    @Inject
    lateinit var imageLoader: ImageLoader
    @Inject
    lateinit var settingsDataStore: SettingsDataStore
    @Inject
    lateinit var connectToServerUseCase: ConnectToServerUseCase
    @Inject
    lateinit var getConnectionStateUseCase: GetConnectionStateUseCase
    @Inject
    lateinit var downloadManager: DownloadManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        SingletonImageLoader.setSafe { imageLoader }
        downloadManager.start()
        applicationScope.launch {
            val serverUrl = settingsDataStore.getServerUrl().first()
            if (serverUrl.isBlank()) return@launch
            val state = getConnectionStateUseCase().value
            if (state !is ConnectionState.Disconnected) return@launch
            val token = settingsDataStore.getAuthToken().first().trim()
            Logger.i(TAG, "Auto-connecting to ${Logger.sanitizeUrl(serverUrl)}")
            connectToServerUseCase(serverUrl, token, persistSettings = false)
        }
    }

    private companion object {
        private const val TAG = "HarmonixiaApplication"
    }
}
