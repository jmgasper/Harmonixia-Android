package com.harmonixia.android

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.util.ConnectionRecoveryManager
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.PrefetchScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

@HiltAndroidApp
class HarmonixiaApplication : Application() {
    @Inject
    lateinit var imageLoader: ImageLoader
    @Inject
    lateinit var settingsDataStore: SettingsDataStore
    @Inject
    lateinit var getConnectionStateUseCase: GetConnectionStateUseCase
    @Inject
    lateinit var repository: MusicAssistantRepository
    @Inject
    lateinit var prefetchScheduler: PrefetchScheduler
    @Inject
    lateinit var connectionRecoveryManager: ConnectionRecoveryManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        SingletonImageLoader.setSafe { imageLoader }
        connectionRecoveryManager.start()
        applicationScope.launch {
            val serverUrl = settingsDataStore.getServerUrl().first()
            if (serverUrl.isBlank()) return@launch
            getConnectionStateUseCase()
                .filterIsInstance<ConnectionState.Connected>()
                .first()
            val albums = repository.fetchRecentlyPlayed(RECENT_PREFETCH_LIMIT)
                .getOrDefault(emptyList())
            val playlists = repository.fetchRecentlyPlayedPlaylists(RECENT_PREFETCH_LIMIT)
                .getOrDefault(emptyList())
            if (albums.isNotEmpty() || playlists.isNotEmpty()) {
                Logger.i(TAG, "Scheduling detail cache warming")
                prefetchScheduler.scheduleDetailPrefetch(albums, playlists)
            }
        }
    }

    private companion object {
        private const val TAG = "HarmonixiaApplication"
        private const val RECENT_PREFETCH_LIMIT = 10
    }
}
