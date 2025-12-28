package com.harmonixia.android.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.harmonixia.android.domain.model.EqBandConfig
import com.harmonixia.android.domain.model.EqSettings
import com.harmonixia.android.util.Logger
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class EqDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    suspend fun saveEqSettings(settings: EqSettings) {
        dataStore.edit { preferences ->
            preferences[EQ_ENABLED_KEY] = settings.enabled
            if (settings.selectedPresetId == null) {
                preferences.remove(EQ_PRESET_ID_KEY)
            } else {
                preferences[EQ_PRESET_ID_KEY] = settings.selectedPresetId
            }
            if (settings.customBands == null) {
                preferences.remove(EQ_CUSTOM_BANDS_KEY)
            } else {
                val payload = json.encodeToString(
                    ListSerializer(EqBandConfig.serializer()),
                    settings.customBands
                )
                preferences[EQ_CUSTOM_BANDS_KEY] = payload
            }
        }
    }

    fun getEqSettings(): Flow<EqSettings> {
        return dataStore.data.map { preferences ->
            val enabled = preferences[EQ_ENABLED_KEY] ?: false
            val selectedPresetId = preferences[EQ_PRESET_ID_KEY]
            val customBands = preferences[EQ_CUSTOM_BANDS_KEY]?.let { payload ->
                runCatching {
                    json.decodeFromString(
                        ListSerializer(EqBandConfig.serializer()),
                        payload
                    )
                }.getOrElse {
                    Logger.w(TAG, "Failed to decode EQ bands", it)
                    null
                }
            }
            EqSettings(
                enabled = enabled,
                selectedPresetId = selectedPresetId,
                customBands = customBands
            )
        }
    }

    suspend fun clearEqSettings() {
        dataStore.edit { preferences ->
            preferences.remove(EQ_ENABLED_KEY)
            preferences.remove(EQ_PRESET_ID_KEY)
            preferences.remove(EQ_CUSTOM_BANDS_KEY)
        }
        Logger.i(TAG, "Cleared EQ settings")
    }

    companion object {
        val EQ_ENABLED_KEY = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET_ID_KEY = stringPreferencesKey("eq_preset_id")
        val EQ_CUSTOM_BANDS_KEY = stringPreferencesKey("eq_custom_bands")
        private const val TAG = "EqDataStore"
    }
}
