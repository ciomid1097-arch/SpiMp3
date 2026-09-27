package com.spimp3.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spimp3.app.data.Song
import com.spimp3.app.data.formatDuration
import com.spimp3.app.data.formatSize
import com.spimp3.app.ui.theme.accentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenuSheet(
    song: Song?,
    isFavorite: Boolean,
    inPlaylistId: Long? = null,
    onDismiss: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onShare: (Song) -> Unit,
    onEdit: (Song) -> Unit,
    onDelete: (Song) -> Unit,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
    onShowInfo: (Song) -> Unit,
) {
    if (song == null) return
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(song.albumId, song.title, Modifier.size(52.dp), cornerRadius = 10.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(
            Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
        SheetItem(Icons.Rounded.SkipNext, "Play next") { onPlayNext(song) }
        SheetItem(Icons.Rounded.Queue, "Add to queue") { onAddToQueue(song) }
        SheetItem(Icons.Rounded.PlaylistAdd, "Add to playlist") { onAddToPlaylist(song) }
        SheetItem(
            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (isFavorite) "Remove from favorites" else "Add to favorites",
        ) { onToggleFavorite(song) }
        if (onRemoveFromPlaylist != null) {
            SheetItem(Icons.Rounded.PlaylistAdd, "Remove from this playlist") {
                onRemoveFromPlaylist(song)
            }
        }
        HorizontalDivider(
            Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
        SheetItem(Icons.Outlined.Edit, "Edit details", highlight = true) { onEdit(song) }
        SheetItem(Icons.Rounded.Share, "Share") { onShare(song) }
        SheetItem(Icons.Outlined.Info, "Song info") { showInfo = true }
        SheetItem(
            Icons.Outlined.Delete,
            "Delete from device",
            tint = MaterialTheme.colorScheme.error,
        ) { showDeleteConfirm = true }
        Spacer(Modifier.height(16.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete this song?") },
            text = {
                Text(
                    "\"${song.title}\" will be permanently removed from your device. " +
                        "This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(song)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Song info") },
            text = {
                Column {
                    InfoLine("Title", song.title)
                    InfoLine("Artist", song.artist)
                    InfoLine("Album", song.album)
                    InfoLine("Duration", formatDuration(song.durationMs))
                    InfoLine("Size", formatSize(song.sizeBytes))
                    InfoLine("Year", song.year?.toString() ?: "—")
                    InfoLine("Track", song.track?.toString() ?: "—")
                    InfoLine("Format", song.fileName.substringAfterLast('.', "—").uppercase())
                    InfoLine("Path", song.folder)
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.65f),
        )
    }
}

@Composable
private fun SheetItem(
    icon: ImageVector,
    label: String,
    highlight: Boolean = false,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val iconTint = tint ?: if (highlight) accentColor() else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (tint != null) tint else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
