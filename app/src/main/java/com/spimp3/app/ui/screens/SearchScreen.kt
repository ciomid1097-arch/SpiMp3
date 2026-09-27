package com.spimp3.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spimp3.app.MainViewModel
import com.spimp3.app.data.Song
import com.spimp3.app.ui.components.Artwork
import com.spimp3.app.ui.components.ArtworkFallback
import com.spimp3.app.ui.components.SelectionUniverse
import com.spimp3.app.ui.components.SongRow
import com.spimp3.app.ui.theme.accentColor
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onOpenMenu: (Song) -> Unit,
    onOpenAlbum: (Long) -> Unit = {},
    onOpenArtist: (Long) -> Unit = {},
) {
    val library by vm.library.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val player = vm.player
    val currentId by player.currentSongId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val q = query.trim().lowercase()
    val songs = remember(q, library.songs) {
        if (q.isEmpty()) emptyList()
        else library.songs.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }.take(50)
    }
    val albums = remember(q, library.albums) {
        if (q.isEmpty()) emptyList() else library.albums.filter { it.title.lowercase().contains(q) }.take(6)
    }
    val artists = remember(q, library.artists) {
        if (q.isEmpty()) emptyList() else library.artists.filter { it.name.lowercase().contains(q) }.take(6)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Songs, artists, albums…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
        }

        if (q.isEmpty()) return@Column

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (artists.isNotEmpty()) {
                item { SectionLabel("Artists") }
                items(artists, key = { "sa${it.id}" }) { artist ->
                    val rep = library.songsByArtist[artist.id]?.firstOrNull()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArtist(artist.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(rep?.albumId ?: -1L, artist.name, Modifier.size(44.dp), cornerRadius = 22.dp, fallback = ArtworkFallback.INITIAL)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(artist.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${artist.trackCount} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Rounded.Person, null, tint = accentColor().copy(alpha = 0.6f))
                    }
                }
            }
            if (albums.isNotEmpty()) {
                item { SectionLabel("Albums") }
                items(albums, key = { "sb${it.id}" }) { album ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAlbum(album.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(album.id, album.title, Modifier.size(44.dp), cornerRadius = 8.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(album.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                album.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Rounded.Album, null, tint = accentColor().copy(alpha = 0.6f))
                    }
                }
            }
            if (songs.isNotEmpty()) {
                item {
                    SelectionUniverse(songs.map { it.id })
                    SectionLabel("Songs")
                }
                items(songs, key = { "sc${it.id}" }) { s ->
                    SongRow(
                        song = s,
                        isCurrent = s.id == currentId,
                        isPlaying = isPlaying,
                        isFavorite = s.id in favorites,
                        showAlbum = true,
                        onClick = { onSongClick(s, songs) },
                        onOverflow = { onOpenMenu(s) },
                    )
                }
            }
            if (songs.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
                item {
                    Text(
                        "No results for \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
    )
}
