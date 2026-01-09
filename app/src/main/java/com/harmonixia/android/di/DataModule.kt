package com.harmonixia.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.harmonixia.android.data.local.LocalMediaDatabase
import com.harmonixia.android.data.local.LocalMediaScanner
import com.harmonixia.android.data.local.dao.CachedAlbumDao
import com.harmonixia.android.data.local.dao.CachedArtistDao
import com.harmonixia.android.data.local.dao.LocalAlbumDao
import com.harmonixia.android.data.local.dao.LocalArtistDao
import com.harmonixia.android.data.local.dao.LocalTrackDao
import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.data.local.EqPresetCache
import com.harmonixia.android.data.local.EqPresetParser
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.remote.MusicAssistantWebSocketClient
import com.harmonixia.android.data.repository.LocalMediaRepositoryImpl
import com.harmonixia.android.data.repository.MusicAssistantRepositoryImpl
import com.harmonixia.android.data.repository.OfflineLibraryRepositoryImpl
import com.harmonixia.android.domain.repository.LocalMediaRepository
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
    abstract fun bindOfflineLibraryRepository(
        impl: OfflineLibraryRepositoryImpl
    ): OfflineLibraryRepository

    @Binds
    @Singleton
    abstract fun bindLocalMediaRepository(
        impl: LocalMediaRepositoryImpl
    ): LocalMediaRepository

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
        fun provideLocalMediaDatabase(
            @ApplicationContext context: Context
        ): LocalMediaDatabase {
            return Room.databaseBuilder(
                context,
                LocalMediaDatabase::class.java,
                LocalMediaDatabase.DATABASE_NAME
            )
                .addMigrations(
                    LocalMediaDatabase.MIGRATION_1_2,
                    LocalMediaDatabase.MIGRATION_2_3,
                    LocalMediaDatabase.MIGRATION_3_4
                )
                .build()
        }

        @Provides
        @Singleton
        fun provideLocalTrackDao(
            database: LocalMediaDatabase
        ): LocalTrackDao = database.localTrackDao()

        @Provides
        @Singleton
        fun provideLocalAlbumDao(
            database: LocalMediaDatabase
        ): LocalAlbumDao = database.localAlbumDao()

        @Provides
        @Singleton
        fun provideLocalArtistDao(
            database: LocalMediaDatabase
        ): LocalArtistDao = database.localArtistDao()

        @Provides
        @Singleton
        fun provideCachedAlbumDao(
            database: LocalMediaDatabase
        ): CachedAlbumDao = database.cachedAlbumDao()

        @Provides
        @Singleton
        fun provideCachedArtistDao(
            database: LocalMediaDatabase
        ): CachedArtistDao = database.cachedArtistDao()

        @Provides
        @Singleton
        fun provideLocalMediaScanner(
            localTrackDao: LocalTrackDao,
            localAlbumDao: LocalAlbumDao,
            localArtistDao: LocalArtistDao,
            database: LocalMediaDatabase,
            @ApplicationContext context: Context,
            ioDispatcher: CoroutineDispatcher
        ): LocalMediaScanner = LocalMediaScanner(
            localTrackDao,
            localAlbumDao,
            localArtistDao,
            database,
            context,
            ioDispatcher
        )

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
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
