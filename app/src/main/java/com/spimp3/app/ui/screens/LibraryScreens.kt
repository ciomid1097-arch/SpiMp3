package com.spimp3.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spimp3.app.MainViewModel
import com.spimp3.app.data.Album
import com.spimp3.app.data.Artist
import com.spimp3.app.data.MusicFolder
import com.spimp3.app.data.Song
import com.spimp3.app.data.formatDuration
import com.spimp3.app.ui.components.Artwork
import com.spimp3.app.ui.components.ArtworkFallback
import com.spimp3.app.ui.components.RefreshableBox
import com.spimp3.app.ui.components.SelectionUniverse
import com.spimp3.app.ui.components.SongRow
import com.spimp3.app.ui.theme.accentColor

/** Which slice of the library the Songs tab is showing. */
private enum class SongFilter(val label: String) { ALL("All"), FAVORITES("Favorites"), RECENT("Recent") }

@Composable
fun SongsScreen(vm: MainViewModel, onSongClick: (Song, List<Song>) -> Unit, onOpenMenu: (Song) -> Unit) {
    val library by vm.library.collectAsState()
    val settings by vm.settings.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val recents by vm.recents.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val player = vm.player
    val currentId by player.currentSongId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    var filter by remember { mutableStateOf(SongFilter.ALL) }

    val sorted = remember(library.songs, settings.sortOrder, filter, favorites, recents) {
        val base = when (filter) {
            SongFilter.ALL -> library.songs
            SongFilter.FAVORITES -> library.songs.filter { it.id in favorites }
            SongFilter.RECENT -> recents.mapNotNull { library.songById[it] }
        }
        if (filter == SongFilter.RECENT) base else sortSongs(base, settings.sortOrder)
    }

    SelectionUniverse(sorted.map { it.id })

    Column(Modifier.fillMaxSize()) {
        LibraryHeader(
            title = when (filter) {
                SongFilter.ALL -> "Songs"
                SongFilter.FAVORITES -> "Favorites"
                SongFilter.RECENT -> "Recently played"
            },
            subtitle = "${sorted.size} ${if (sorted.size == 1) "track" else "tracks"}",
            vm = vm,
        )

        // Favourites live here, not as a stray heart on every row.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SongFilter.entries.toList()) { f ->
                FilterPill(
                    label = f.label,
                    count = when (f) {
                        SongFilter.ALL -> library.songs.size
                        SongFilter.FAVORITES -> favorites.size
                        SongFilter.RECENT -> recents.size
                    },
                    selected = filter == f,
                    onClick = { filter = f },
                )
            }
        }

        RefreshableBox(isRefreshing = refreshing, onRefresh = { vm.refreshLibrary() }) {
            if (sorted.isEmpty()) {
                EmptyFilter(filter, Modifier.fillMaxSize())
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(sorted, key = { it.id }) { s ->
                        SongRow(
                            song = s,
                            isCurrent = s.id == currentId,
                            isPlaying = isPlaying,
                            showAlbum = true,
                            onClick = { onSongClick(s, sorted) },
                            onOverflow = { onOpenMenu(s) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val accent = accentColor()
    Box(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(
                if (selected) accent.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = if (count > 0) "$label ($count)" else label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyFilter(filter: SongFilter, modifier: Modifier = Modifier) {
    val message = when (filter) {
        SongFilter.FAVORITES ->
            "No favorites yet.\nOpen the ••• menu on any song and choose \"Add to favorites\"."
        SongFilter.RECENT -> "Nothing played yet."
        SongFilter.ALL -> "No music found on this device."
    }
    Box(modifier.padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AlbumsScreen(vm: MainViewModel, onOpenAlbum: (Long) -> Unit) {
    val library by vm.library.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    Column(Modifier.fillMaxSize()) {
        LibraryHeader(title = "Albums", subtitle = "${library.albums.size} albums", vm = vm)
        RefreshableBox(isRefreshing = refreshing, onRefresh = { vm.refreshLibrary() }) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(library.albums, key = { it.id }) { album ->
                    AlbumCard(album) { onOpenAlbum(album.id) }
                }
            }
        }
    }
}

@Composable
fun ArtistsScreen(vm: MainViewModel, onOpenArtist: (Long) -> Unit) {
    val library by vm.library.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    Column(Modifier.fillMaxSize()) {
        LibraryHeader(title = "Artists", subtitle = "${library.artists.size} artists", vm = vm)
        RefreshableBox(isRefreshing = refreshing, onRefresh = { vm.refreshLibrary() }) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(library.artists, key = { it.id }) { artist ->
                    val rep = library.songsByArtist[artist.id]?.firstOrNull()
                    Column(
                        Modifier.clickable { onOpenArtist(artist.id) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Artwork(
                            rep?.albumId ?: -1L,
                            artist.name,
                            Modifier.size(110.dp),
                            cornerRadius = 55.dp,
                            fallback = ArtworkFallback.INITIAL,
                        )
                        Text(
                            artist.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            "${artist.trackCount} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun FoldersScreen(vm: MainViewModel, onOpenFolder: (String) -> Unit) {
    val library by vm.library.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    Column(Modifier.fillMaxSize()) {
        LibraryHeader(title = "Folders", subtitle = "${library.folders.size} folders", vm = vm)
        RefreshableBox(isRefreshing = refreshing, onRefresh = { vm.refreshLibrary() }) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(library.folders, key = { it.path }) { folder ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFolder(folder.path) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor().copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Folder, null, tint = accentColor())
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(folder.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                folder.path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            "${folder.trackCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Artwork(album.id, album.title, Modifier.fillMaxWidth().height(150.dp), cornerRadius = 12.dp)
        Text(
            album.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LibraryHeader(title: String, subtitle: String, vm: MainViewModel) {
    var sortMenu by remember { mutableStateOf(false) }
    val settings by vm.settings.collectAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { sortMenu = true }) {
            Icon(Icons.Rounded.Sort, contentDescription = "Sort")
        }
        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
            com.spimp3.app.data.SettingsStore.SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = {
                        Text(
                            order.label,
                            color = if (order == settings.sortOrder) accentColor() else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        vm.setSortOrder(order)
                        sortMenu = false
                    },
                )
            }
        }
    }
}

internal fun sortSongs(songs: List<Song>, order: com.spimp3.app.data.SettingsStore.SortOrder): List<Song> = when (order) {
    com.spimp3.app.data.SettingsStore.SortOrder.TITLE -> songs.sortedBy { it.title.lowercase() }
    com.spimp3.app.data.SettingsStore.SortOrder.ARTIST -> songs.sortedBy { it.artist.lowercase() }
    com.spimp3.app.data.SettingsStore.SortOrder.ALBUM -> songs.sortedBy { it.album.lowercase() }
    com.spimp3.app.data.SettingsStore.SortOrder.RECENT -> songs.sortedByDescending { it.dateAdded }
    com.spimp3.app.data.SettingsStore.SortOrder.DURATION -> songs.sortedByDescending { it.durationMs }
}
