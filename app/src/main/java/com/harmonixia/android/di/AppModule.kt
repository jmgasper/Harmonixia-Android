package com.harmonixia.android.di

import android.content.Context
import androidx.work.WorkManager
import com.harmonixia.android.data.local.DownloadSettingsDataStore
import com.harmonixia.android.domain.manager.DownloadManager
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.util.NetworkConnectivityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context = context

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideDownloadManager(
        workManager: WorkManager,
        downloadRepository: DownloadRepository,
        downloadSettingsDataStore: DownloadSettingsDataStore
    ): DownloadManager = DownloadManager(workManager, downloadRepository, downloadSettingsDataStore)

    @Provides
    @Singleton
    fun provideNetworkConnectivityManager(
        @ApplicationContext context: Context
    ): NetworkConnectivityManager = NetworkConnectivityManager(context)
}
