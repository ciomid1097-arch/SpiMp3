package com.spimp3.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spimp3.app.data.Song
import com.spimp3.app.data.formatDuration
import com.spimp3.app.ui.theme.accentColor

/**
 * One song in a list. Favouriting is deliberately *not* here — it lives in the
 * overflow menu, so the row stays clean and the three-dot button owns all
 * secondary actions.
 *
 * A long press starts multi-select mode (see [LocalSelection]); while that mode
 * is active a tap toggles the row instead of playing it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onOverflow: () -> Unit,
    showAlbum: Boolean = false,
) {
    val selection = LocalSelection.current
    val isSelected = selection.active && song.id in selection.selected

    Column(
        Modifier.background(
            if (isSelected) accentColor().copy(alpha = 0.14f)
            else androidx.compose.ui.graphics.Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (selection.active) selection.onToggle(song.id) else onClick() },
                    onLongClick = { selection.onStart(song.id) },
                    onLongClickLabel = "Select",
                )
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectableArtwork(
                selected = isSelected,
                selectionMode = selection.active,
            ) {
                Artwork(
                    albumId = song.albumId,
                    title = song.title,
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 8.dp,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCurrent) accentColor() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent && isPlaying) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = accentColor(),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(12.dp),
                        )
                    }
                    Text(
                        text = if (showAlbum) "${song.artist} • ${song.album}" else song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = formatDuration(song.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onOverflow) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 76.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
    }
}

/**
 * Wraps the artwork with a tick badge while multi-select is running. The badge
 * sits on the corner so the cover art itself is never obscured.
 */
@Composable
private fun SelectableArtwork(
    selected: Boolean,
    selectionMode: Boolean,
    content: @Composable () -> Unit,
) {
    if (!selectionMode) {
        content()
        return
    }
    val accent = accentColor()
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        content()
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (selected) accent
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                )
                .then(
                    if (selected) Modifier
                    else Modifier.background(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        CircleShape,
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
