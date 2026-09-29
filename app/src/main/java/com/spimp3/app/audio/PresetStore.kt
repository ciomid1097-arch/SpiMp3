package com.spimp3.app.audio

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.fxDataStore: DataStore<Preferences> by preferencesDataStore(name = "spimp3_fx")

/**
 * Persistent storage for user-saved equalizer presets. Only presets live here —
 * the master Enable toggle is deliberately session-only (AudioFxController) and
 * is never written to disk, so every app start begins with the EQ off.
 */
class PresetStore(private val context: Context) {

    @Serializable
    data class StoredPreset(
        val name: String,
        val bands: List<Float>,
        val bass: Int = 0,
        val treble: Int = 0,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val KEY = stringPreferencesKey("user_presets_json")

    val presets: Flow<List<StoredPreset>> = context.fxDataStore.data.map { p ->
        p[KEY]?.let { runCatching { json.decodeFromString<List<StoredPreset>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun save(preset: StoredPreset) {
        context.fxDataStore.edit { p ->
            val cur = p[KEY]?.let { runCatching { json.decodeFromString<List<StoredPreset>>(it) }.getOrNull() } ?: emptyList()
            p[KEY] = (listOf(preset) + cur.filter { it.name != preset.name }).let { json.encodeToString(it) }
        }
    }

    suspend fun delete(name: String) {
        context.fxDataStore.edit { p ->
            val cur = p[KEY]?.let { runCatching { json.decodeFromString<List<StoredPreset>>(it) }.getOrNull() } ?: emptyList()
            p[KEY] = json.encodeToString(cur.filter { it.name != name })
        }
    }

    suspend fun toControllerPreset(s: StoredPreset) = AudioFxController.Preset(
        name = s.name, bandDb = s.bands, bass = s.bass, treble = s.treble,
    )
}
