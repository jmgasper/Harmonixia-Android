@file:Suppress("DEPRECATION")

package com.harmonixia.android.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.harmonixia.android.domain.model.AuthMethod
import com.harmonixia.android.util.LibraryViewMode
import com.harmonixia.android.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val ENCRYPTED_SETTINGS_FILE_NAME = "secure_settings"
private const val ENCRYPTED_PASSWORD_KEY = "encrypted_password"

class SettingsDataStore @Inject constructor(
    @ApplicationContext context: Context,
    private val dataStore: DataStore<Preferences>,
    private val passwordStorage: PasswordStorage =
        EncryptedSharedPreferencesPasswordStorage(context.applicationContext)
) {
    private val applicationLabel = context.applicationContext.packageName
    private val passwordMigrationMutex = Mutex()

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

    suspend fun saveAuthMethod(method: AuthMethod) {
        dataStore.edit { preferences ->
            preferences[AUTH_METHOD_KEY] = method.name
        }
    }

    suspend fun saveUsername(username: String) {
        dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    suspend fun savePassword(password: String) {
        if (password.isEmpty()) {
            passwordStorage.clearPassword()
        } else {
            passwordStorage.writePassword(password)
        }
        dataStore.edit { preferences ->
            preferences.remove(PASSWORD_KEY)
        }
    }

    suspend fun saveSendspinClientId(clientId: String) {
        dataStore.edit { preferences ->
            preferences[SENDSPIN_CLIENT_ID_KEY] = clientId
        }
    }

    suspend fun clearSendspinClientId() {
        dataStore.edit { preferences ->
            preferences[SENDSPIN_CLIENT_ID_KEY] = ""
        }
    }

    suspend fun saveLocalMediaFolderUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[LOCAL_MEDIA_FOLDER_URI_KEY] = uri
        }
    }

    suspend fun saveAlbumsViewMode(mode: LibraryViewMode) {
        dataStore.edit { preferences ->
            preferences[ALBUMS_VIEW_MODE_KEY] = mode.name
        }
    }

    suspend fun savePlaylistsViewMode(mode: LibraryViewMode) {
        dataStore.edit { preferences ->
            preferences[PLAYLISTS_VIEW_MODE_KEY] = mode.name
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

    fun getAuthMethod(): Flow<AuthMethod> {
        return dataStore.data.map { preferences ->
            val stored = preferences[AUTH_METHOD_KEY]
            if (stored.isNullOrBlank()) {
                AuthMethod.TOKEN
            } else {
                runCatching { AuthMethod.valueOf(stored) }.getOrNull() ?: AuthMethod.TOKEN
            }
        }
    }

    fun getUsername(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[USERNAME_KEY] ?: ""
        }
    }

    fun getPassword(): Flow<String> {
        return dataStore.data.map { preferences ->
            val encryptedPassword = passwordStorage.readPassword().orEmpty()
            if (encryptedPassword.isNotEmpty()) {
                return@map encryptedPassword
            }

            val legacyPassword = preferences[PASSWORD_KEY].orEmpty()
            if (legacyPassword.isEmpty()) {
                return@map ""
            }

            migrateLegacyPassword(legacyPassword)
            passwordStorage.readPassword().orEmpty().ifEmpty { legacyPassword }
        }
    }

    fun getSendspinClientId(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[SENDSPIN_CLIENT_ID_KEY] ?: ""
        }
    }

    fun getAlbumsViewMode(): Flow<LibraryViewMode> {
        return dataStore.data.map { preferences ->
            parseViewMode(preferences[ALBUMS_VIEW_MODE_KEY])
        }
    }

    fun getPlaylistsViewMode(): Flow<LibraryViewMode> {
        return dataStore.data.map { preferences ->
            parseViewMode(preferences[PLAYLISTS_VIEW_MODE_KEY])
        }
    }

    suspend fun clearSettings() {
        passwordStorage.clearPassword()
        dataStore.edit { preferences ->
            preferences.clear()
        }
        Logger.i(TAG, "Cleared settings for $applicationLabel")
    }

    companion object {
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        val AUTH_METHOD_KEY = stringPreferencesKey("auth_method")
        val USERNAME_KEY = stringPreferencesKey("username")
        val PASSWORD_KEY = stringPreferencesKey("password")
        val SENDSPIN_CLIENT_ID_KEY = stringPreferencesKey("sendspin_client_id")
        val LOCAL_MEDIA_FOLDER_URI_KEY = stringPreferencesKey("local_media_folder_uri")
        val ALBUMS_VIEW_MODE_KEY = stringPreferencesKey("albums_view_mode")
        val PLAYLISTS_VIEW_MODE_KEY = stringPreferencesKey("playlists_view_mode")
        private const val TAG = "SettingsDataStore"
    }

    private fun parseViewMode(value: String?): LibraryViewMode {
        if (value.isNullOrBlank()) return LibraryViewMode.AUTO
        return runCatching { LibraryViewMode.valueOf(value.trim().uppercase()) }
            .getOrElse { LibraryViewMode.AUTO }
    }

    private suspend fun migrateLegacyPassword(legacyPassword: String) {
        passwordMigrationMutex.withLock {
            if (!passwordStorage.readPassword().isNullOrEmpty()) {
                return
            }

            passwordStorage.writePassword(legacyPassword)
            dataStore.edit { preferences ->
                preferences.remove(PASSWORD_KEY)
            }
            Logger.i(TAG, "Migrated legacy password to encrypted storage for $applicationLabel")
        }
    }
}

interface PasswordStorage {
    fun readPassword(): String?
    fun writePassword(password: String)
    fun clearPassword()
}

@Suppress("DEPRECATION")
private class EncryptedSharedPreferencesPasswordStorage(
    context: Context
) : PasswordStorage {
    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_SETTINGS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun readPassword(): String? = sharedPreferences.getString(ENCRYPTED_PASSWORD_KEY, null)

    override fun writePassword(password: String) {
        sharedPreferences.edit()
            .putString(ENCRYPTED_PASSWORD_KEY, password)
            .apply()
    }

    override fun clearPassword() {
        sharedPreferences.edit()
            .remove(ENCRYPTED_PASSWORD_KEY)
            .apply()
    }
}
