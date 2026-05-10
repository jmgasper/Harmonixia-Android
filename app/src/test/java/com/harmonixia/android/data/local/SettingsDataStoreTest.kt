package com.harmonixia.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SettingsDataStoreTest {

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStoreFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var passwordStorage: FakePasswordStorage
    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setUp() {
        val applicationContext = mockk<Context>()
        val context = mockk<Context>()
        every { context.applicationContext } returns applicationContext
        every { applicationContext.packageName } returns "com.harmonixia.android.test"

        dataStoreFile = File.createTempFile("settings-${UUID.randomUUID()}", ".preferences_pb")
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) { dataStoreFile }

        passwordStorage = FakePasswordStorage()
        settingsDataStore = SettingsDataStore(context, dataStore, passwordStorage)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    @Test
    fun savePassword_writesEncryptedValueAndRemovesLegacyDataStoreKey() = runBlocking {
        dataStore.edit { preferences ->
            preferences[SettingsDataStore.PASSWORD_KEY] = "legacy-password"
        }

        settingsDataStore.savePassword("new-password")

        assertEquals("new-password", settingsDataStore.getPassword().first())
        assertEquals("new-password", passwordStorage.readPassword())
        assertNull(dataStore.data.first()[SettingsDataStore.PASSWORD_KEY])
    }

    @Test
    fun getPassword_migratesLegacyPlaintextPasswordToEncryptedStorage() = runBlocking {
        dataStore.edit { preferences ->
            preferences[SettingsDataStore.PASSWORD_KEY] = "legacy-password"
        }

        assertEquals("legacy-password", settingsDataStore.getPassword().first())
        assertEquals("legacy-password", passwordStorage.readPassword())
        assertNull(dataStore.data.first()[SettingsDataStore.PASSWORD_KEY])
    }

    @Test
    fun savePassword_emptyValueClearsEncryptedAndLegacyStorage() = runBlocking {
        settingsDataStore.savePassword("keep-me")

        settingsDataStore.savePassword("")

        assertEquals("", settingsDataStore.getPassword().first())
        assertNull(passwordStorage.readPassword())
        assertNull(dataStore.data.first()[SettingsDataStore.PASSWORD_KEY])
    }

    private class FakePasswordStorage : PasswordStorage {
        private var encryptedPassword: String? = null

        override fun readPassword(): String? = encryptedPassword

        override fun writePassword(password: String) {
            encryptedPassword = password
        }

        override fun clearPassword() {
            encryptedPassword = null
        }
    }
}
