package com.harmonixia.android.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadSettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun saveMaxConcurrentDownloads(count: Int) {
        dataStore.edit { preferences ->
            preferences[MAX_CONCURRENT_DOWNLOADS_KEY] = count
        }
    }

    fun getMaxConcurrentDownloads(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[MAX_CONCURRENT_DOWNLOADS_KEY] ?: DEFAULT_MAX_CONCURRENT_DOWNLOADS
        }
    }

    suspend fun saveDownloadQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[DOWNLOAD_QUALITY_KEY] = quality
        }
    }

    fun getDownloadQuality(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[DOWNLOAD_QUALITY_KEY] ?: DEFAULT_DOWNLOAD_QUALITY
        }
    }

    suspend fun saveDownloadOverWifiOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DOWNLOAD_OVER_WIFI_ONLY_KEY] = enabled
        }
    }

    fun getDownloadOverWifiOnly(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[DOWNLOAD_OVER_WIFI_ONLY_KEY] ?: DEFAULT_DOWNLOAD_OVER_WIFI_ONLY
        }
    }

    companion object {
        val MAX_CONCURRENT_DOWNLOADS_KEY = intPreferencesKey("max_concurrent_downloads")
        val DOWNLOAD_QUALITY_KEY = stringPreferencesKey("download_quality")
        val DOWNLOAD_OVER_WIFI_ONLY_KEY = booleanPreferencesKey("download_over_wifi_only")
        private const val DEFAULT_MAX_CONCURRENT_DOWNLOADS = 3
        private const val DEFAULT_DOWNLOAD_QUALITY = "high"
        private const val DEFAULT_DOWNLOAD_OVER_WIFI_ONLY = false
    }
}
