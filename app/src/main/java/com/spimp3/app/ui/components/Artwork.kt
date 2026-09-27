package com.spimp3.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.spimp3.app.data.MusicRepository
import com.spimp3.app.ui.theme.accentColor

/** What to draw when a track has no embedded cover art. */
enum class ArtworkFallback {
    /** A music note — the clearest signal that this is a song without artwork. */
    NOTE,

    /** First letter of the title — used for artist/album avatars where the
     *  initial actually carries information. */
    INITIAL,
}

@Composable
fun Artwork(
    albumId: Long,
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: ArtworkFallback = ArtworkFallback.NOTE,
) {
    val bg = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(MusicRepository.albumArtUri(albumId))
                .crossfade(true)
                .build(),
            contentDescription = title,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            loading = { Placeholder(fallback, title) },
            error = { Placeholder(fallback, title) },
        )
    }
}

@Composable
private fun Placeholder(fallback: ArtworkFallback, title: String) {
    val accent = accentColor()
    val grad = Brush.linearGradient(listOf(accent.copy(alpha = 0.14f), Color.Transparent))
    Box(
        Modifier
            .fillMaxSize()
            .background(grad),
        contentAlignment = Alignment.Center,
    ) {
        if (fallback == ArtworkFallback.INITIAL) {
            Text(
                text = title.trim().take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = accent.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .size(28.dp),
            )
        }
    }
}
