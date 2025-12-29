package com.harmonixia.android.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OfflineLibraryRepository
import com.harmonixia.android.service.playback.AudioDeviceManager
import com.harmonixia.android.service.playback.EqualizerManager
import com.harmonixia.android.service.playback.MediaLibraryBrowser
import com.harmonixia.android.service.playback.PlaybackNotificationManager
import com.harmonixia.android.service.playback.PlaybackService
import com.harmonixia.android.service.playback.PlaybackServiceConnection
import com.harmonixia.android.service.playback.PlaybackStateManager
import com.harmonixia.android.service.playback.QueueManager
import com.harmonixia.android.service.playback.SendspinPlaybackManager
import com.harmonixia.android.service.playback.VolumeHandler
import com.harmonixia.android.util.NetworkConnectivityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
@UnstableApi
object ServiceModule {

    @Provides
    @Singleton
    fun provideQueueManager(
        repository: MusicAssistantRepository,
        downloadRepository: DownloadRepository
    ): QueueManager = QueueManager(repository, downloadRepository)

    @Provides
    @Singleton
    fun provideMediaLibraryBrowser(
        repository: MusicAssistantRepository,
        downloadRepository: DownloadRepository,
        offlineLibraryRepository: OfflineLibraryRepository,
        networkConnectivityManager: NetworkConnectivityManager
    ): MediaLibraryBrowser = MediaLibraryBrowser(
        repository,
        downloadRepository,
        offlineLibraryRepository,
        networkConnectivityManager
    )

    @Provides
    @Singleton
    fun providePlaybackStateManager(
        repository: MusicAssistantRepository,
        queueManager: QueueManager
    ): PlaybackStateManager =
        PlaybackStateManager(repository, queueManager, Dispatchers.IO, Dispatchers.Main.immediate)

    @Provides
    @Singleton
    fun provideAudioDeviceManager(
        @ApplicationContext context: Context
    ): AudioDeviceManager = AudioDeviceManager(context)

    @Provides
    @Singleton
    fun providePlaybackNotificationManager(
        @ApplicationContext context: Context
    ): PlaybackNotificationManager = PlaybackNotificationManager(context)

    @Provides
    @Singleton
    fun providePlaybackServiceConnection(
        @ApplicationContext context: Context
    ): PlaybackServiceConnection = PlaybackServiceConnection(context)

    @Provides
    @Singleton
    fun provideVolumeHandler(
        @ApplicationContext context: Context,
        repository: MusicAssistantRepository,
        audioDeviceManager: AudioDeviceManager
    ): VolumeHandler = VolumeHandler(context, repository, audioDeviceManager)

    @Provides
    @Singleton
    fun provideSendspinPlaybackManager(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        json: Json,
        settingsDataStore: SettingsDataStore,
        equalizerManager: EqualizerManager
    ): SendspinPlaybackManager =
        SendspinPlaybackManager(context, okHttpClient, json, settingsDataStore, equalizerManager)
}

@Module
@InstallIn(ServiceComponent::class)
@UnstableApi
object PlaybackServiceBindings {
    @Provides
    fun providePlaybackService(service: PlaybackService): PlaybackService = service
}
