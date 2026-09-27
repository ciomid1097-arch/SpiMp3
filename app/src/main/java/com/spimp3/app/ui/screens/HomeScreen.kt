package com.spimp3.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spimp3.app.MainViewModel
import com.spimp3.app.data.Song
import com.spimp3.app.data.UpdateChecker
import com.spimp3.app.data.WhatsNew
import com.spimp3.app.data.formatDuration
import com.spimp3.app.ui.components.Artwork
import com.spimp3.app.ui.components.ArtworkFallback
import com.spimp3.app.ui.components.RefreshableBox
import com.spimp3.app.ui.components.SelectionUniverse
import com.spimp3.app.ui.components.SongRow
import com.spimp3.app.ui.components.UpdateCard
import com.spimp3.app.ui.components.WhatsNewDialog
import com.spimp3.app.ui.theme.accentColor
import java.util.Calendar

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onOpenMenu: (Song) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (Long) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onShowList: (title: String, songs: List<Song>) -> Unit,
    onShuffleAll: () -> Unit,
) {
    val library by vm.library.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val recents by vm.recents.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val player = vm.player
    val currentId by player.currentSongId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val context = LocalContext.current

    var showNewPlaylist by remember { mutableStateOf(false) }
    var editPlaylist by remember { mutableStateOf<com.spimp3.app.data.Playlist?>(null) }

    // ---- Updates: remote check + local "What's new" ----
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        if (vm.updateCheckEnabled.value) {
            updateInfo = UpdateChecker.check(com.spimp3.app.BuildConfig.VERSION_CODE)
        }
    }
    var showWhatsNew by remember { mutableStateOf(false) }
    var whatsNewNotes by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(vm.lastSeenVersion.value) {
        val code = com.spimp3.app.BuildConfig.VERSION_CODE
        // An in-place upgrade = the package was updated after it was first
        // installed (firstInstallTime != lastUpdateTime). Covers users whose
        // previous version never recorded a marker, without bothering fresh
        // installs. The marker then keeps repeats away on later launches.
        val upgraded = runCatching {
            val pm = context.packageManager
            val t = pm.getPackageInfo(context.packageName, 0)
            t.firstInstallTime != t.lastUpdateTime
        }.getOrDefault(false)
        val unseen = vm.lastSeenVersion.value in 1 until code
        if ((unseen || upgraded) && code > 1) {
            WhatsNew.notes[code]?.let { notes ->
                whatsNewNotes = notes
                showWhatsNew = true
            }
            vm.markVersionSeen(code)
        }
    }
    val favoritesSongs = remember(library.songs, favorites) { library.songs.filter { it.id in favorites } }
    val recentSongs = remember(library.songs, recents) {
        recents.mapNotNull { id -> library.songById[id] }
    }
    val recentAdded = remember(library.songs) { library.songs.sortedByDescending { it.dateAdded }.take(20) }
    val topArtists = remember(library.artists) { library.artists.sortedByDescending { it.trackCount }.take(10) }

    RefreshableBox(
        isRefreshing = refreshing,
        onRefresh = { vm.refreshLibrary() },
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
    ) {
        item {
            val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
            val greeting = when (hour) {
                in 5..11 -> "Good morning"
                in 12..17 -> "Good afternoon"
                in 18..21 -> "Good evening"
                else -> "Late night listening"
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        greeting,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("SpiMp3", style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                }
            }
        }

        updateInfo?.let { info ->
            item {
                UpdateCard(
                    info = info,
                    onUpdate = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(info.apkUrl),
                        )
                        runCatching { context.startActivity(intent) }
                    },
                )
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickChip(
                    icon = Icons.Rounded.Favorite,
                    label = "Favorites",
                    count = favoritesSongs.size,
                    modifier = Modifier.weight(1f),
                ) { onShowList("Favorites", favoritesSongs) }
                QuickChip(
                    icon = Icons.Rounded.History,
                    label = "Recents",
                    count = recentSongs.size,
                    modifier = Modifier.weight(1f),
                ) { onShowList("Recently played", recentSongs) }
                QuickChip(
                    icon = Icons.Rounded.Shuffle,
                    label = "Shuffle all",
                    count = library.songs.size,
                    modifier = Modifier.weight(1f),
                ) { onShuffleAll() }
            }
        }

        if (recentSongs.isNotEmpty()) {
            item {
                SectionHeader("Jump back in")
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(recentSongs, key = { "r${it.id}" }) { s ->
                        Column(
                            Modifier
                                .width(120.dp)
                                .clickable {
                                    val idx = recentSongs.indexOf(s)
                                    onSongClick(s, recentSongs)
                                },
                        ) {
                            Artwork(s.albumId, s.title, Modifier.size(120.dp), cornerRadius = 14.dp)
                            Text(
                                s.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Text(
                                s.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        if (topArtists.isNotEmpty()) {
            item {
                SectionHeader("Artists you listen to")
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(topArtists, key = { "a${it.id}" }) { artist ->
                        Column(
                            Modifier
                                .width(96.dp)
                                .clickable { onOpenArtist(artist.id) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val rep = library.songsByArtist[artist.id]?.firstOrNull()
                            Artwork(
                                rep?.albumId ?: -1L,
                                artist.name,
                                Modifier.size(96.dp),
                                cornerRadius = 48.dp,
                                fallback = ArtworkFallback.INITIAL,
                            )
                            Text(
                                artist.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                "Your playlists",
                trailing = { TextButton(onClick = { showNewPlaylist = true }) { Text("New") } },
            )
        }
        if (playlists.isEmpty()) {
            item {
                Text(
                    "No playlists yet. Select some songs, tap Playlist, and pick \"New playlist\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }
        }
        items(playlists.size, key = { "p${playlists[it].id}" }) { i ->
            val pl = playlists[i]
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlaylist(pl.id) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.QueueMusic, null, tint = accentColor())
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(pl.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${pl.songIds.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { editPlaylist = pl }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist options")
                }
            }
        }

        if (recentAdded.isNotEmpty()) {
            item {
                SelectionUniverse(recentAdded.map { it.id })
                SectionHeader("Recently added")
            }
            items(recentAdded.size) { i ->
                val s = recentAdded[i]
                SongRow(
                    song = s,
                    isCurrent = s.id == currentId,
                    isPlaying = isPlaying,
                    isFavorite = s.id in favorites,
                    showAlbum = true,
                    onClick = { onSongClick(s, recentAdded) },
                    onOverflow = { onOpenMenu(s) },
                )
            }
        }

        if (library.songs.isEmpty() && !library.loading) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No music found", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Copy some audio files to your device, then pull to refresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showNewPlaylist) {
        NewPlaylistDialog(
            onDismiss = { showNewPlaylist = false },
            onCreate = { name ->
                vm.createPlaylist(name)
                showNewPlaylist = false
            },
        )
    }

    if (showWhatsNew) {
        WhatsNewDialog(
            versionLabel = com.spimp3.app.BuildConfig.VERSION_NAME,
            notes = whatsNewNotes,
            onDismiss = { showWhatsNew = false },
        )
    }

    editPlaylist?.let { pl ->
        PlaylistOptionsDialog(
            playlist = pl,
            onDismiss = { editPlaylist = null },
            onRename = { newName ->
                vm.renamePlaylist(pl, newName)
                editPlaylist = null
            },
            onDelete = {
                vm.deletePlaylist(pl)
                editPlaylist = null
            },
        )
    }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun QuickChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = accentColor())
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Rename / delete a playlist from its ⋮ on the home list. */
@Composable
private fun PlaylistOptionsDialog(
    playlist: com.spimp3.app.data.Playlist,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var deleting by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var name by remember(playlist.id) { mutableStateOf(playlist.name) }

    when {
        renaming -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Rename playlist") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Playlist name") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { onRename(name) },
                        enabled = name.isNotBlank() && name != playlist.name,
                    ) { Text("Rename") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            )
        }
        deleting -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Delete \"${playlist.name}\"?") },
                text = { Text("The playlist is removed. The songs themselves stay on your device.") },
                confirmButton = {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            )
        }
        else -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(playlist.name) },
                text = { Text("${playlist.songIds.size} songs") },
                confirmButton = {
                    TextButton(onClick = { renaming = true }) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { deleting = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
            )
        }
    }
}

@Composable
private fun NewPlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
