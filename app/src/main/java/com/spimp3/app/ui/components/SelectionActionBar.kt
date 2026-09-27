package com.spimp3.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spimp3.app.ui.theme.accentColor
import com.spimp3.app.ui.theme.onAccentColor

/**
 * Contextual bar that replaces the mini player while songs are selected.
 * Every action applies to the whole selection at once.
 */
@Composable
fun SelectionActionBar(
    count: Int,
    allSelected: Boolean,
    allFavorited: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Header: how many, and a way out
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Cancel selection")
            }
            Text(
                text = if (count == 1) "1 song selected" else "$count songs selected",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            Text(
                text = if (allSelected) "Clear" else "All",
                style = MaterialTheme.typography.labelLarge,
                color = accentColor(),
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onSelectAll)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(4.dp))

        // Primary action, full width
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor())
                .clickable(onClick = onPlay)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = onAccentColor(),
                )
                Text(
                    text = "  Play",
                    style = MaterialTheme.typography.titleSmall,
                    color = onAccentColor(),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Secondary actions
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Action(Icons.Rounded.Queue, "Queue", onAddToQueue)
            Action(
                if (allFavorited) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (allFavorited) "Unfav" else "Fav",
                onToggleFavorite,
                accent = allFavorited,
            )
            Action(Icons.Rounded.PlaylistAdd, "Playlist", onAddToPlaylist)
            Action(Icons.Rounded.Share, "Share", onShare)
            Action(Icons.Rounded.Delete, "Delete", onDelete, tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Action(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    tint: Color? = null,
) {
    val iconTint = tint ?: if (accent) accentColor() else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = iconTint)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = iconTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
