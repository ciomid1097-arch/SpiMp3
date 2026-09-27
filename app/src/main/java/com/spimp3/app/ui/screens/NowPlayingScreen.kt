package com.spimp3.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.spimp3.app.MainViewModel
import com.spimp3.app.data.LyricsParser
import com.spimp3.app.data.MusicRepository
import com.spimp3.app.data.formatDuration
import com.spimp3.app.ui.theme.accentColor
import com.spimp3.app.ui.theme.onAccentColor
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onSongClick: (com.spimp3.app.data.Song, List<com.spimp3.app.data.Song>) -> Unit,
    onOpenMenu: (com.spimp3.app.data.Song) -> Unit,
) {
    val player = vm.player
    val songId by player.currentSongId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val position by player.positionMs.collectAsState()
    val duration by player.durationMs.collectAsState()
    val shuffle by player.shuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val speed by player.speed.collectAsState()
    val queue by player.queue.collectAsState()
    val currentIndex by player.currentIndex.collectAsState()
    val library by vm.library.collectAsState()
    val favorites by vm.favorites.collectAsState()

    val song = songId?.let { library.songById[it] }
    var showQueue by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    val lyrics = remember(song?.data, showLyrics) {
        if (!showLyrics) null else LyricsParser.parse(LyricsParser.lrcFileFor(song?.data ?: ""))
    }
    val activeLine = remember(lyrics, position) {
        lyrics?.let { LyricsParser.activeLineIndex(it, position) }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(activeLine) {
        if (lyrics != null && activeLine != null && activeLine >= 0) {
            val target = (activeLine - 3).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Blurred artwork backdrop
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song?.albumId?.let { MusicRepository.albumArtUri(it) })
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(60.dp),
            error = {},
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.82f)),
                    ),
                ),
        )

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ExpandMore, contentDescription = "Collapse", Modifier.size(30.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Now Playing",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showSleep = true }) {
                    Icon(Icons.Rounded.Bedtime, contentDescription = "Sleep timer", tint = Color.White)
                }
            }

            // Artwork
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                val artModifier = Modifier
                    .fillMaxWidth(0.78f)
                    .clip(RoundedCornerShape(24.dp))
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song?.albumId?.let { MusicRepository.albumArtUri(it) })
                        .crossfade(true)
                        .build(),
                    contentDescription = song?.title,
                    contentScale = ContentScale.Crop,
                    modifier = artModifier,
                    error = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                song?.title?.take(1)?.uppercase() ?: "?",
                                style = MaterialTheme.typography.displaySmall,
                                color = accentColor(),
                            )
                        }
                    },
                )
            }

            // Title / artist
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    song?.title ?: "Nothing playing",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song?.artist ?: "—",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }

            // Seek bar
            Column(Modifier.padding(top = 18.dp)) {
                Slider(
                    value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                    onValueChange = { player.seekTo((it * duration).toLong()) },
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = accentColor(),
                        activeTrackColor = accentColor(),
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    ),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        formatDuration(position),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }

            // Transport controls
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { player.setShuffle(!shuffle) }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffle) accentColor() else Color.White.copy(alpha = 0.75f),
                    )
                }
                IconButton(onClick = { player.previous() }) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", Modifier.size(38.dp), tint = Color.White)
                }
                Box(
                    Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(accentColor())
                        .clickable { player.togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        Modifier.size(38.dp),
                        tint = onAccentColor(),
                    )
                }
                IconButton(onClick = { player.next() }) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", Modifier.size(38.dp), tint = Color.White)
                }
                IconButton(onClick = { player.cycleRepeatMode() }) {
                    Icon(
                        when (repeatMode) {
                            androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                            androidx.media3.common.Player.REPEAT_MODE_ALL -> Icons.Rounded.Repeat
                            else -> Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) accentColor() else Color.White.copy(alpha = 0.75f),
                    )
                }
            }

            // Secondary row: lyrics / speed / queue / favorite
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) accentColor() else Color.White.copy(alpha = 0.75f),
                    )
                }
                IconButton(onClick = { showSpeed = true }) {
                    Icon(Icons.Rounded.Speed, contentDescription = "Speed", tint = Color.White.copy(alpha = 0.75f))
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = "Queue", tint = Color.White.copy(alpha = 0.75f))
                }
                val fav = song?.id in favorites
                IconButton(onClick = { song?.let { vm.toggleFavorite(it.id) } }) {
                    Icon(
                        if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (fav) accentColor() else Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }

        // ---- Sheets ----
        if (showLyrics) {
            ModalBottomSheet(
                onDismissRequest = { showLyrics = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    "Lyrics",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                )
                if (lyrics == null) {
                    Text(
                        "No .lrc file found next to this track. Put a .lrc file with the same name as the audio file in the same folder.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                } else {
                    LazyColumn(state = listState, modifier = Modifier.height(420.dp), contentPadding = PaddingValues(bottom = 40.dp)) {
                        items(lyrics.size) { i ->
                            val line = lyrics[i]
                            val isActive = i == activeLine
                            Text(
                                text = line.text.ifBlank { "♪" },
                                style = if (isActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                                color = when {
                                    isActive -> accentColor()
                                    i < (activeLine ?: 0) -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }

        if (showSpeed) {
            ModalBottomSheet(onDismissRequest = { showSpeed = false }, containerColor = MaterialTheme.colorScheme.surface) {
                Text("Playback speed", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 28.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                        FilledTonalButton(onClick = { player.setSpeed(s); showSpeed = false }) {
                            Text(
                                "${s}x",
                                color = if (s == speed) accentColor() else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        if (showSleep) {
            ModalBottomSheet(onDismissRequest = { showSleep = false }, containerColor = MaterialTheme.colorScheme.surface) {
                Text("Sleep timer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    listOf(15, 30, 45, 60).forEach { m ->
                        FilledTonalButton(onClick = { player.setSleepTimer(m); showSleep = false }) {
                            Text("${m}m")
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    FilledTonalButton(onClick = { player.setSleepTimer(0); showSleep = false }) {
                        Text("End of queue")
                    }
                    FilledTonalButton(onClick = { player.setSleepTimer(-1); showSleep = false }) {
                        Text("Cancel timer")
                    }
                }
            }
        }

        if (showQueue) {
            ModalBottomSheet(onDismissRequest = { showQueue = false }, containerColor = MaterialTheme.colorScheme.surface) {
                Text("Queue", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))
                LazyColumn(Modifier.height(460.dp)) {
                    items(queue.size) { i ->
                        val item = queue[i]
                        val isCur = i == currentIndex
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { player.seekToIndex(i); showQueue = false }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${i + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCur) accentColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(28.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.mediaMetadata.title?.toString() ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isCur) accentColor() else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    item.mediaMetadata.artist?.toString() ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            IconButton(onClick = { player.removeFromQueue(i) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
