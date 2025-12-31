package com.harmonixia.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.harmonixia.android.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsDataStore @Inject constructor(
    @ApplicationContext context: Context,
    private val dataStore: DataStore<Preferences>
) {
    private val applicationLabel = context.applicationContext.packageName

    suspend fun saveServerUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = url
        }
    }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    suspend fun saveSendspinClientId(clientId: String) {
        dataStore.edit { preferences ->
            preferences[SENDSPIN_CLIENT_ID_KEY] = clientId
        }
    }

    suspend fun saveLocalMediaFolderUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[LOCAL_MEDIA_FOLDER_URI_KEY] = uri
        }
    }

    fun getServerUrl(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[SERVER_URL_KEY] ?: ""
        }
    }

    fun getLocalMediaFolderUri(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[LOCAL_MEDIA_FOLDER_URI_KEY] ?: ""
        }
    }

    fun getAuthToken(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[AUTH_TOKEN_KEY] ?: ""
        }
    }

    fun getSendspinClientId(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[SENDSPIN_CLIENT_ID_KEY] ?: ""
        }
    }

    suspend fun clearSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        Logger.i(TAG, "Cleared settings for $applicationLabel")
    }

    companion object {
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        val SENDSPIN_CLIENT_ID_KEY = stringPreferencesKey("sendspin_client_id")
        val LOCAL_MEDIA_FOLDER_URI_KEY = stringPreferencesKey("local_media_folder_uri")
        private const val TAG = "SettingsDataStore"
    }
}
