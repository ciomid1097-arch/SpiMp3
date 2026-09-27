package com.spimp3.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spimp3.app.MainViewModel
import com.spimp3.app.data.Song
import com.spimp3.app.ui.components.SelectionUniverse
import com.spimp3.app.ui.components.SongRow
import com.spimp3.app.ui.theme.accentColor
import com.spimp3.app.ui.theme.onAccentColor

/** Generic list screen for smart lists (Favorites, Recently played, …). */
@Composable
fun SmartListScreen(
    title: String,
    songIds: List<Long>,
    vm: MainViewModel,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onOpenMenu: (Song) -> Unit,
) {
    val library by vm.library.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val player = vm.player
    val currentId by player.currentSongId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    val songs = songIds.mapNotNull { library.songById[it] }
    SelectionUniverse(songs.map { it.id })

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 4.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        if (songs.isEmpty()) {
            Text(
                "Nothing here yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
            return@Column
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Button(
                onClick = { onSongClick(songs.first(), songs) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor(),
                    contentColor = onAccentColor(),
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text("Play")
            }
            OutlinedButton(
                onClick = {
                    val shuffled = songs.shuffled()
                    onSongClick(shuffled.first(), shuffled)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Icon(Icons.Rounded.Shuffle, null)
                Text("Shuffle", Modifier.padding(start = 6.dp))
            }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(songs, key = { it.id }) { s ->
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
    }
}
