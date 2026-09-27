package com.spimp3.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spimp3.app.MainViewModel
import com.spimp3.app.data.Playlist
import com.spimp3.app.data.Song
import com.spimp3.app.data.formatDuration
import com.spimp3.app.ui.components.Artwork
import com.spimp3.app.ui.components.SelectionUniverse
import com.spimp3.app.ui.components.SongRow
import com.spimp3.app.ui.theme.accentColor
import com.spimp3.app.ui.theme.onAccentColor

@Composable
fun AlbumDetailScreen(
    vm: MainViewModel,
    albumId: Long,
    onSongClick: (Song, List<Song>) -> Unit,
    onOpenMenu: (Song) -> Unit,
    onOpenArtist: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val library by vm.library.collectAsState()
    val album = library.albumById[albumId] ?: return
    val songs = remember(albumId, library.songs) {
        library.songsByAlbum[albumId].orEmpty().sortedBy { it.track ?: 0 }
    }
    val favorites by vm.favorites.collectAsState()
    val player = vm.player
    val currentId by player.currentSongId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    DetailScaffold(
        title = album.title,
        subtitle = album.artist,
        meta = listOfNotNull(
            album.year?.toString(),
            "${songs.size} tracks",
            formatDuration(songs.sumOf { it.durationMs }),
        ).joinToString(" • "),
        albumId = album.id,
        onBack = onBack,
        onPlayAll = { if (songs.isNotEmpty()) onSongClick(songs.first(), songs) },
        onShuffle = {
            val s = songs.shuffled()
            onSongClick(s.first(), s)
        },
        songList = songs,
        currentId = currentId,
        isPlaying = isPlaying,
        favorites = favorites,
        onSongClick = onSongClick,
        onOpenMenu = onOpenMenu,
        onToggleFavorite = { vm.toggleFavorite(it.id) },
        onOpenArtist = { onOpenArtist(album.artistIdRef(songs)) },
    )
}

private fun com.spimp3.app.data.Album.artistIdRef(songs: List<Song>): Long = songs.firstOrNull()?.artistId ?: -1L

@Composable
fun ArtistDetailScreen(
    vm: MainViewModel,
    artistId: Long,
    onSongClick: (Song, List<Song>) -> Unit,
    onOpenMenu: (Song) -> Unit,
    onBack: () -> Unit,
) {
    val library by vm.library.collectAsState()
    val artist = library.artistById[artistId] ?: return
    val songs = remember(artistId, library.songs) { library.songsByArtist[artistId].orEmpty() }
    val favorites by vm.favorites.collectAsState()
    val player = vm.player
    val currentId by player.currentSongId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    DetailScaffold(
        title = artist.name,
        subtitle = "Artist",
        meta = "${artist.albumCount} albums • ${songs.size} songs",
        albumId = songs.firstOrNull()?.albumId ?: -1L,
        onBack = onBack,
        onPlayAll = { if (songs.isNotEmpty()) onSongClick(songs.first(), songs) },
        onShuffle = { val s = songs.shuffled(); onSongClick(s.first(), s) },
        songList = songs,
        currentId = currentId,
        isPlaying = isPlaying,
        favorites = favorites,
        onSongClick = onSongClick,
        onOpenMenu = onOpenMenu,
        onToggleFavorite = { vm.toggleFavorite(it.id) },
        onOpenArtist = null,
    )
}

@Composable
fun FolderDetailScreen(
    vm: MainViewModel,
    folderPath: String,
    onSongClick: (Song, List<Song>) -> Unit,
    onOpenMenu: (Song) -> Unit,
    onBack: () -> Unit,
) {
    val library by vm.library.collectAsState()
    val songs = remember(folderPath, library.songs) { library.songsByFolder[folderPath].orEmpty() }
    val favorites by vm.favorites.collectAsState()
    val player = vm.player
    val currentId by player.currentSongId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    DetailScaffold(
        title = folderPath.substringAfterLast('/'),
        subtitle = folderPath,
        meta = "${songs.size} tracks",
        albumId = songs.firstOrNull()?.albumId ?: -1L,
        onBack = onBack,
        onPlayAll = { if (songs.isNotEmpty()) onSongClick(songs.first(), songs) },
        onShuffle = { if (songs.isNotEmpty()) { val s = songs.shuffled(); onSongClick(s.first(), s) } },
        songList = songs,
        currentId = currentId,
        isPlaying = isPlaying,
        favorites = favorites,
        onSongClick = onSongClick,
        onOpenMenu = onOpenMenu,
        onToggleFavorite = { vm.toggleFavorite(it.id) },
        onOpenArtist = null,
    )
}

@Composable
fun PlaylistDetailScreen(
    vm: MainViewModel,
    playlistId: Long,
    onSongClick: (Song, List<Song>) -> Unit,
    onOpenMenu: (Song) -> Unit,
    onBack: () -> Unit,
) {
    val library by vm.library.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val playlist = playlists.firstOrNull { it.id == playlistId } ?: return
    val songs = remember(playlist.songIds, library.songs) {
        playlist.songIds.mapNotNull { library.songById[it] }
    }
    val favorites by vm.favorites.collectAsState()
    val player = vm.player
    val currentId by player.currentSongId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    DetailScaffold(
        title = playlist.name,
        subtitle = "Playlist",
        meta = "${songs.size} tracks",
        albumId = songs.firstOrNull()?.albumId ?: -1L,
        onBack = onBack,
        onPlayAll = { if (songs.isNotEmpty()) onSongClick(songs.first(), songs) },
        onShuffle = { if (songs.isNotEmpty()) { val s = songs.shuffled(); onSongClick(s.first(), s) } },
        songList = songs,
        currentId = currentId,
        isPlaying = isPlaying,
        favorites = favorites,
        onSongClick = onSongClick,
        onOpenMenu = onOpenMenu,
        onToggleFavorite = { vm.toggleFavorite(it.id) },
        onOpenArtist = null,
    )
}

@Composable
private fun DetailScaffold(
    title: String,
    subtitle: String,
    meta: String,
    albumId: Long,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    songList: List<Song>,
    currentId: Long?,
    isPlaying: Boolean,
    favorites: Set<Long>,
    onSongClick: (Song, List<Song>) -> Unit,
    onOpenMenu: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOpenArtist: (() -> Unit)?,
) {
    Box(Modifier.fillMaxSize()) {
        // Gradient backdrop
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor().copy(alpha = 0.28f), MaterialTheme.colorScheme.background),
                    ),
                ),
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Artwork(
                        albumId,
                        title,
                        Modifier
                            .size(180.dp)
                            .align(Alignment.CenterHorizontally),
                        cornerRadius = 18.dp,
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        Modifier.padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onPlayAll,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor(),
                                contentColor = onAccentColor(),
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null)
                            Text("Play", Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(onClick = onShuffle, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Shuffle, null)
                            Text("Shuffle", Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
            item { SelectionUniverse(songList.map { it.id }) }
            items(songList, key = { "${title.hashCode()}-${it.id}" }) { s ->
                SongRow(
                    song = s,
                    isCurrent = s.id == currentId,
                    isPlaying = isPlaying,
                    isFavorite = s.id in favorites,
                    onClick = { onSongClick(s, songList) },
                    onOverflow = { onOpenMenu(s) },
                )
            }
        }
    }
}
