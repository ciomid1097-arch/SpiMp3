package com.spimp3.app

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spimp3.app.data.MusicRepository
import com.spimp3.app.data.Playlist
import com.spimp3.app.data.SettingsStore
import com.spimp3.app.playback.PlayerConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.Manifest

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repository = MusicRepository(app)
    val settingsStore = SettingsStore(app)

    val player = PlayerConnection(app, viewModelScope)

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val library = repository.library

    val settings = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsStore.Settings())

    val playlists: StateFlow<List<Playlist>> = settingsStore.playlists
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<Set<Long>> = settingsStore.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val recents: StateFlow<List<Long>> = settingsStore.recents
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refreshPermission()
        viewModelScope.launch { player.connect() }
    }

    fun refreshPermission() {
        val granted = if (Build.VERSION.SDK_INT >= 33) {
            getApplication<Application>().checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            @Suppress("DEPRECATION")
            getApplication<Application>().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
        _hasPermission.value = granted
        viewModelScope.launch { repository.refresh(granted) }
    }

    // ---- commands ----

    /** Pull-to-refresh: rescan MediaStore (picks up external edits/deletions). */
    fun refreshLibrary() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            val granted = _hasPermission.value
            repository.refresh(granted)
            // keep the spinner visible long enough to read as intentional
            delay(550)
            _refreshing.value = false
        }
    }

    fun toggleFavorite(songId: Long) = viewModelScope.launch { settingsStore.toggleFavorite(songId) }

    fun pushRecent(songId: Long) = viewModelScope.launch { settingsStore.pushRecent(songId) }

    fun createPlaylist(name: String, songIds: List<Long> = emptyList(), onDone: (Playlist) -> Unit = {}) =
        viewModelScope.launch {
            val pl = Playlist(
                id = System.currentTimeMillis(),
                name = name.ifBlank { "New Playlist" },
                songIds = songIds,
            )
            settingsStore.savePlaylists(playlists.value + pl)
            onDone(pl)
        }

    fun renamePlaylist(playlist: Playlist, newName: String) = viewModelScope.launch {
        settingsStore.savePlaylists(
            playlists.value.map { if (it.id == playlist.id) it.copy(name = newName) else it },
        )
    }

    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch {
        settingsStore.savePlaylists(playlists.value.filter { it.id != playlist.id })
    }

    fun addToPlaylist(playlist: Playlist, songIds: List<Long>) = viewModelScope.launch {
        settingsStore.savePlaylists(
            playlists.value.map {
                if (it.id == playlist.id) it.copy(songIds = (it.songIds + songIds).distinct()) else it
            },
        )
    }

    fun removeFromPlaylist(playlist: Playlist, songId: Long) = viewModelScope.launch {
        settingsStore.savePlaylists(
            playlists.value.map {
                if (it.id == playlist.id) it.copy(songIds = it.songIds - songId) else it
            },
        )
    }

    fun setSortOrder(order: SettingsStore.SortOrder) = viewModelScope.launch { settingsStore.setSortOrder(order) }
    fun setAccent(accent: String) = viewModelScope.launch { settingsStore.setAccent(accent) }
    fun setThemeMode(mode: String) = viewModelScope.launch { settingsStore.setThemeMode(mode) }

    fun setFavorites(ids: Set<Long>, favorite: Boolean) =
        viewModelScope.launch { settingsStore.setFavorites(ids, favorite) }
    fun setGapless(value: Boolean) = viewModelScope.launch { settingsStore.setGapless(value) }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
