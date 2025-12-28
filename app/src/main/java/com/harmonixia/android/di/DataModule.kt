package com.harmonixia.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.data.local.EqPresetCache
import com.harmonixia.android.data.local.EqPresetParser
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.data.remote.MusicAssistantWebSocketClient
import com.harmonixia.android.data.repository.MusicAssistantRepositoryImpl
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
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
    }
}
