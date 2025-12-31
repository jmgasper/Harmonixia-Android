package com.harmonixia.android

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.harmonixia.android.data.local.LocalMediaScanner
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.remote.ConnectionState
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
    lateinit var localMediaScanner: LocalMediaScanner

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        SingletonImageLoader.setSafe { imageLoader }
        applicationScope.launch {
            val serverUrl = settingsDataStore.getServerUrl().first()
            if (serverUrl.isBlank()) return@launch
            val state = getConnectionStateUseCase().value
            if (state !is ConnectionState.Disconnected) return@launch
            val token = settingsDataStore.getAuthToken().first().trim()
            Logger.i(TAG, "Auto-connecting to ${Logger.sanitizeUrl(serverUrl)}")
            connectToServerUseCase(serverUrl, token, persistSettings = false)
        }
        applicationScope.launch {
            val folderUri = settingsDataStore.getLocalMediaFolderUri().first()
            if (folderUri.isNotBlank()) {
                Logger.i(TAG, "Starting local media scan")
                localMediaScanner.scanFolder(folderUri)
                    .onSuccess { result ->
                        Logger.i(
                            TAG,
                            "Local media scan complete: ${result.tracksAdded} tracks, " +
                                "${result.albumsAdded} albums, ${result.artistsAdded} artists"
                        )
                    }
                    .onFailure { error ->
                        Logger.e(TAG, "Local media scan failed", error)
                    }
            }
        }
    }

    private companion object {
        private const val TAG = "HarmonixiaApplication"
    }
}
