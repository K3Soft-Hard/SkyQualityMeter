package com.pk3ju.skyqualitymeter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sqm_settings_v9_5")

class SqmPreferencesManager(private val context: Context) {
    
    companion object {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val ACTIVE_SERVER_ID = stringPreferencesKey("active_server_id")
        val TEXT_SCALE = floatPreferencesKey("global_text_scale")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val IS_FULLSCREEN = booleanPreferencesKey("is_fullscreen")
        val POLLING_INTERVAL_MS = longPreferencesKey("polling_interval_ms")
        
        val DEV_MODE_UNLOCKED = booleanPreferencesKey("dev_mode_unlocked")
        val BETA_CAPTURE_UI = booleanPreferencesKey("beta_capture_ui")
        val AUTO_NAME_CAPTURE = booleanPreferencesKey("auto_name_capture")
        
        val SERVERS_JSON = stringPreferencesKey("servers_json_v9")
        val SAVED_OBSERVATIONS_JSON = stringPreferencesKey("saved_observations_json_v9")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true; coerceInputValues = true }

    val isFirstLaunchFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_FIRST_LAUNCH] ?: true }
    val activeServerIdFlow: Flow<String> = context.dataStore.data.map { it[ACTIVE_SERVER_ID] ?: "default" }
    val textScaleFlow: Flow<Float> = context.dataStore.data.map { it[TEXT_SCALE] ?: 1.0f }
    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_DARK_THEME] ?: true }
    val isFullscreenFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_FULLSCREEN] ?: false }
    val pollingIntervalMsFlow: Flow<Long> = context.dataStore.data.map { it[POLLING_INTERVAL_MS] ?: 3000L }
    val devModeUnlockedFlow: Flow<Boolean> = context.dataStore.data.map { it[DEV_MODE_UNLOCKED] ?: false }
    val betaCaptureUiFlow: Flow<Boolean> = context.dataStore.data.map { it[BETA_CAPTURE_UI] ?: false }
    val autoNameCaptureFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_NAME_CAPTURE] ?: false }
    
    val serversFlow: Flow<List<ServerProfile>> = context.dataStore.data.map { prefs ->
        val jsonString = prefs[SERVERS_JSON] ?: "[]"
        try { json.decodeFromString(jsonString) } catch (e: Exception) { emptyList() }
    }

    val savedObservationsFlow: Flow<List<SavedObservation>> = context.dataStore.data.map { prefs ->
        val jsonString = prefs[SAVED_OBSERVATIONS_JSON] ?: "[]"
        try { json.decodeFromString(jsonString) } catch (e: Exception) { emptyList() }
    }

    suspend fun updatePreferences(action: suspend (MutablePreferences) -> Unit) {
        context.dataStore.edit { action(it) }
    }

    suspend fun saveServers(servers: List<ServerProfile>) {
        val jsonString = json.encodeToString(servers)
        context.dataStore.edit { it[SERVERS_JSON] = jsonString }
    }

    suspend fun saveObservations(obs: List<SavedObservation>) {
        val jsonString = json.encodeToString(obs)
        context.dataStore.edit { it[SAVED_OBSERVATIONS_JSON] = jsonString }
    }
}
