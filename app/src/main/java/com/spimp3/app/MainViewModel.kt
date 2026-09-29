package com.spimp3.app

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spimp3.app.data.MusicRepository
import com.spimp3.app.data.Playlist
import com.spimp3.app.data.SettingsStore
import com.spimp3.app.audio.AudioFxController
import com.spimp3.app.audio.DeviceVolumeController
import com.spimp3.app.audio.PresetStore
import com.spimp3.app.playback.PlayerConnection
import androidx.compose.runtime.mutableStateOf
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

    // ---- Equalizer (audiofx) ----
    val audioFx = AudioFxController()
    val fxPresets = PresetStore(app)
    val deviceVolume = DeviceVolumeController(app)

    val userFxPresets: StateFlow<List<PresetStore.StoredPreset>> = fxPresets.presets
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveFxPreset(preset: PresetStore.StoredPreset) =
        viewModelScope.launch { fxPresets.save(preset) }

    fun deleteFxPreset(name: String) =
        viewModelScope.launch { fxPresets.delete(name) }

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

    val updateCheckEnabled: StateFlow<Boolean> = settingsStore.updateCheckEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val lastSeenVersion: StateFlow<Int> = settingsStore.lastSeenVersion
        .stateIn(viewModelScope, SharingStarted.Eagerly, -1)

    // Session-scoped UI flags: computed once per app run, never re-armed by
    // navigation, so dialogs and banners cannot pop up again on every visit
    // to Home.
    var whatsNewShownThisRun by mutableStateOf(false)
        private set
    var updateDismissedThisRun by mutableStateOf(false)
        private set
    private var updateCheckDone = false

    fun markWhatsNewShown() {
        whatsNewShownThisRun = true
    }

    fun dismissUpdateBanner() {
        updateDismissedThisRun = true
    }

    /** Runs the remote update check once per app run (not per navigation). */
    fun checkForUpdateOnce() {
        if (updateCheckDone || !updateCheckEnabled.value) return
        updateCheckDone = true
        viewModelScope.launch {
            val info = com.spimp3.app.data.UpdateChecker.check(
                com.spimp3.app.BuildConfig.VERSION_CODE,
            )
            _updateInfo.value = info
        }
    }

    private val _updateInfo =
        MutableStateFlow<com.spimp3.app.data.UpdateChecker.UpdateInfo?>(null)
    val updateInfo: StateFlow<com.spimp3.app.data.UpdateChecker.UpdateInfo?> =
        _updateInfo.asStateFlow()

    init {
        refreshPermission()
        viewModelScope.launch { player.connect() }
        // Attach the effects chain whenever the player gets a new audio session
        // (ExoPlayer hands out the session id after the first prepare()).
        viewModelScope.launch {
            player.audioSessionId.collect { id ->
                if (id > 0) audioFx.attach(id)
            }
        }
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

    fun setUpdateCheckEnabled(value: Boolean) =
        viewModelScope.launch { settingsStore.setUpdateCheckEnabled(value) }

    fun markVersionSeen(code: Int) =
        viewModelScope.launch { settingsStore.setLastSeenVersion(code) }
    fun setGapless(value: Boolean) = viewModelScope.launch { settingsStore.setGapless(value) }

    override fun onCleared() {
        audioFx.detach()
        deviceVolume.close()
        player.release()
        super.onCleared()
    }
}
