package com.harmonixia.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.harmonixia.android.data.local.DownloadDatabase
import com.harmonixia.android.data.local.DownloadSettingsDataStore
import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.data.local.EqPresetCache
import com.harmonixia.android.data.local.EqPresetParser
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.local.dao.DownloadedAlbumDao
import com.harmonixia.android.data.local.dao.DownloadedPlaylistDao
import com.harmonixia.android.data.local.dao.DownloadedTrackDao
import com.harmonixia.android.data.remote.MusicAssistantWebSocketClient
import com.harmonixia.android.data.repository.DownloadRepositoryImpl
import com.harmonixia.android.data.repository.MusicAssistantRepositoryImpl
import com.harmonixia.android.data.repository.OfflineLibraryRepositoryImpl
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OfflineLibraryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindMusicAssistantRepository(
        impl: MusicAssistantRepositoryImpl
    ): MusicAssistantRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: DownloadRepositoryImpl
    ): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindOfflineLibraryRepository(
        impl: OfflineLibraryRepositoryImpl
    ): OfflineLibraryRepository

    companion object {
        @Provides
        @Singleton
        fun providePreferencesDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> = context.dataStore

        @Provides
        @Singleton
        fun provideSettingsDataStore(
            @ApplicationContext context: Context,
            dataStore: DataStore<Preferences>
        ): SettingsDataStore = SettingsDataStore(context, dataStore)

        @Provides
        @Singleton
        fun provideDownloadSettingsDataStore(
            dataStore: DataStore<Preferences>
        ): DownloadSettingsDataStore = DownloadSettingsDataStore(dataStore)

        @Provides
        @Singleton
        fun provideEqDataStore(
            dataStore: DataStore<Preferences>,
            json: Json
        ): EqDataStore = EqDataStore(dataStore, json)

        @Provides
        @Singleton
        fun provideEqPresetCache(
            @ApplicationContext context: Context,
            okHttpClient: OkHttpClient,
            json: Json
        ): EqPresetCache = EqPresetCache(context, okHttpClient, json)

        @Provides
        @Singleton
        fun provideEqPresetParser(): EqPresetParser = EqPresetParser()

        @Provides
        @Singleton
        fun provideMusicAssistantWebSocketClient(
            okHttpClient: OkHttpClient,
            json: Json
        ): MusicAssistantWebSocketClient = MusicAssistantWebSocketClient(okHttpClient, json)

        @Provides
        @Singleton
        fun provideDownloadDatabase(
            @ApplicationContext context: Context
        ): DownloadDatabase {
            return Room.databaseBuilder(
                context,
                DownloadDatabase::class.java,
                "harmonixia_downloads.db"
            ).fallbackToDestructiveMigration().build()
        }

        @Provides
        fun provideDownloadedTrackDao(
            database: DownloadDatabase
        ): DownloadedTrackDao = database.downloadedTrackDao()

        @Provides
        fun provideDownloadedAlbumDao(
            database: DownloadDatabase
        ): DownloadedAlbumDao = database.downloadedAlbumDao()

        @Provides
        fun provideDownloadedPlaylistDao(
            database: DownloadDatabase
        ): DownloadedPlaylistDao = database.downloadedPlaylistDao()

        @Provides
        @Singleton
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
