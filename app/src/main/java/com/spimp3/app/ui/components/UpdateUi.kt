package com.spimp3.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spimp3.app.data.UpdateChecker
import com.spimp3.app.ui.theme.accentColor
import com.spimp3.app.ui.theme.onAccentColor

/**
 * A slim banner, shown above the bottom navigation when a newer GitHub
 * release exists. Tapping it opens the direct APK download link — nothing
 * else, no account, no store detour.
 */
@Composable
fun UpdateCard(
    info: UpdateChecker.UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onUpdate)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.SystemUpdate,
            contentDescription = null,
            tint = accentColor(),
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                "Update available — ${info.tagName}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Tap to get the new version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Rounded.Download,
            contentDescription = "Download",
            tint = accentColor(),
            modifier = Modifier.padding(end = 8.dp),
        )
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Dismiss until next launch",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * "What's new" dialog after an in-place upgrade. Shown exactly once per
 * release (guarded by the persisted version marker), with a single OK button.
 */
@Composable
fun WhatsNewDialog(
    versionLabel: String,
    notes: List<String>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.SystemUpdate, null, tint = accentColor())
        },
        title = { Text("What's new in $versionLabel") },
        text = {
            Column {
                notes.forEachIndexed { i, line ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(accentColor()),
                        )
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Text(
                    "Thanks for keeping SpiMp3 up to date 💛",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor(),
                    contentColor = onAccentColor(),
                ),
            ) { Text("OK") }
        },
    )
}
