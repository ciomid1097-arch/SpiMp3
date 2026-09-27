package com.spimp3.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spimp3_settings")

/** User settings persisted in DataStore (theme accent, playback prefs, etc.). */
class SettingsStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    object Keys {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val ACCENT = stringPreferencesKey("accent")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val GAPLESS = booleanPreferencesKey("gapless")
        val CROSSFADE = intPreferencesKey("crossfade_sec")
        val REPEAT = stringPreferencesKey("repeat_mode")
        val PLAYLISTS = stringPreferencesKey("playlists_json")
        val FAVORITES = stringPreferencesKey("favorites_json")
        val RECENTS = stringPreferencesKey("recents_json")
    }

    enum class SortOrder(val label: String) {
        TITLE("Title"), ARTIST("Artist"), ALBUM("Album"), RECENT("Recently added"), DURATION("Duration");

        companion object {
            fun from(name: String?) = entries.firstOrNull { it.name == name } ?: TITLE
        }
    }

    data class Settings(
        val sortOrder: SortOrder = SortOrder.TITLE,
        val accent: String = "green",
        val themeMode: String = "dark",
        val dynamicColors: Boolean = false,
        val gapless: Boolean = true,
        val crossfadeSec: Int = 0,
        val repeat: String = "OFF",
    )

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            sortOrder = SortOrder.from(p[Keys.SORT_ORDER]),
            accent = p[Keys.ACCENT] ?: "green",
            themeMode = p[Keys.THEME_MODE] ?: "dark",
            dynamicColors = p[Keys.DYNAMIC_COLORS] ?: false,
            gapless = p[Keys.GAPLESS] ?: true,
            crossfadeSec = p[Keys.CROSSFADE] ?: 0,
            repeat = p[Keys.REPEAT] ?: "OFF",
        )
    }

    suspend fun setSortOrder(order: SortOrder) =
        context.dataStore.edit { it[Keys.SORT_ORDER] = order.name }

    suspend fun setAccent(value: String) =
        context.dataStore.edit { it[Keys.ACCENT] = value }

    suspend fun setThemeMode(value: String) =
        context.dataStore.edit { it[Keys.THEME_MODE] = value }

    suspend fun setDynamicColors(value: Boolean) =
        context.dataStore.edit { it[Keys.DYNAMIC_COLORS] = value }

    suspend fun setGapless(value: Boolean) =
        context.dataStore.edit { it[Keys.GAPLESS] = value }

    suspend fun setCrossfade(sec: Int) =
        context.dataStore.edit { it[Keys.CROSSFADE] = sec }

    suspend fun setRepeat(value: String) =
        context.dataStore.edit { it[Keys.REPEAT] = value }

    // ---- playlists / favorites / recents ----

    val playlists: Flow<List<Playlist>> = context.dataStore.data.map { p ->
        p[Keys.PLAYLISTS]?.let { runCatching { json.decodeFromString<List<Playlist>>(it) }.getOrNull() } ?: emptyList()
    }

    val favorites: Flow<Set<Long>> = context.dataStore.data.map { p ->
        (p[Keys.FAVORITES] ?: "").split(',').mapNotNull { it.toLongOrNull() }.toSet()
    }

    val recents: Flow<List<Long>> = context.dataStore.data.map { p ->
        (p[Keys.RECENTS] ?: "").split(',').mapNotNull { it.toLongOrNull() }
    }

    suspend fun savePlaylists(list: List<Playlist>) =
        context.dataStore.edit { it[Keys.PLAYLISTS] = json.encodeToString(list) }

    suspend fun toggleFavorite(songId: Long) {
        context.dataStore.edit { p ->
            val cur = (p[Keys.FAVORITES] ?: "").split(',').mapNotNull { it.toLongOrNull() }.toSet()
            p[Keys.FAVORITES] = (if (songId in cur) cur - songId else cur + songId).joinToString(",")
        }
    }

    /** Adds or removes a whole selection in one write. */
    suspend fun setFavorites(songIds: Set<Long>, favorite: Boolean) {
        context.dataStore.edit { p ->
            val cur = (p[Keys.FAVORITES] ?: "").split(',').mapNotNull { it.toLongOrNull() }.toSet()
            p[Keys.FAVORITES] = (if (favorite) cur + songIds else cur - songIds).joinToString(",")
        }
    }

    suspend fun pushRecent(songId: Long) {
        context.dataStore.edit { p ->
            val cur = (p[Keys.RECENTS] ?: "").split(',').mapNotNull { it.toLongOrNull() }.toMutableList()
            cur.remove(songId)
            cur.add(0, songId)
            p[Keys.RECENTS] = cur.take(50).joinToString(",")
        }
    }
}
