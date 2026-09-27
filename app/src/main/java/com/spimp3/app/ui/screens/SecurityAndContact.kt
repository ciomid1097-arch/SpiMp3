package com.spimp3.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spimp3.app.ui.theme.accentColor

/**
 * Security & Privacy + contact section, pinned to the very bottom of Settings.
 *
 * Every claim here is factually verifiable by the user (app manifest, Android
 * permission screen, sandbox model), which is what makes it trustworthy.
 */
@Composable
fun SecurityAndContactSection(onOpenLegal: (LegalDoc) -> Unit) {
    val uriHandler = LocalUriHandler.current

    Column(Modifier.padding(bottom = 40.dp)) {

        // ---------- Security ----------
        Text(
            "Security & privacy",
            style = MaterialTheme.typography.labelLarge,
            color = accentColor(),
            modifier = Modifier.padding(start = 20.dp, top = 30.dp, bottom = 4.dp),
        )

        Box(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColor().copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Lock, null, tint = accentColor(), modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "SpiMp3 is offline by design",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Spacer(Modifier.height(14.dp))

                SecurityPoint(
                    "No internet permission",
                    "SpiMp3 does not request the INTERNET permission, so it is technically " +
                        "incapable of opening a network connection. Your music cannot be uploaded, " +
                        "streamed or leaked — the code to do it does not exist in the app.",
                )
                SecurityPoint(
                    "No servers, no account, no tracking",
                    "There is no sign-in, no cloud, no analytics, no advertising and no crash " +
                        "reporting. Nothing about you or your music is ever sent anywhere.",
                )
                SecurityPoint(
                    "Your files are never copied or moved",
                    "SpiMp3 only reads your audio where it already lives. It never duplicates, " +
                        "moves, converts or modifies a file unless you personally use Edit details.",
                )
                SecurityPoint(
                    "Stored data is a private sandbox",
                    "Playlists, favourites and history are kept in the app's own private storage, " +
                        "isolated by Android from every other app on your phone.",
                )
                SecurityPoint(
                    "Built on Android's own protection",
                    "Your music stays in Android's shared storage, which Android isolates per app. " +
                        "Only you and the apps you explicitly grant audio access to can read it.",
                )

                Spacer(Modifier.height(12.dp))

                // Verifiable permission list
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                ) {
                    Column {
                        Text(
                            "Permissions SpiMp3 asks for",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(6.dp))
                        PermissionRow("Audio", "read your music files")
                        PermissionRow("Notifications", "show playback controls")
                        PermissionRow("Foreground service", "keep playing in the background")
                        PermissionRow("Wake lock", "stop playback when the screen turns off")
                        PermissionRow("Network state", "inherited from the playback engine — " +
                            "read-only, cannot transfer data", allowed = false)
                        PermissionRow("Internet", "not requested at all", allowed = false)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.Info,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Don't take our word for it: open Android Settings → Apps → SpiMp3 → " +
                            "Permissions and see the list yourself.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Legal documents, readable offline inside the app.
                Spacer(Modifier.height(6.dp))
                LegalRow(Icons.Rounded.Description, "Privacy policy") {
                    onOpenLegal(LegalDoc.PRIVACY_POLICY)
                }
                LegalRow(Icons.Rounded.Gavel, "Terms of use") {
                    onOpenLegal(LegalDoc.TERMS)
                }
            }
        }

        // ---------- Contact ----------
        Text(
            "Contact the developer",
            style = MaterialTheme.typography.labelLarge,
            color = accentColor(),
            modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp),
        )

        ContactRow(
            icon = Icons.Rounded.AlternateEmail,
            title = "Email",
            value = "workspikestudio@gmail.com",
            onClick = { uriHandler.openUri("mailto:workspikestudio@gmail.com?subject=SpiMp3%20feedback") },
        )
        ContactRow(
            icon = Icons.Rounded.Send,
            title = "Telegram",
            value = "@spike_c",
            onClick = { uriHandler.openUri("https://t.me/spike_c") },
        )

        Spacer(Modifier.height(18.dp))
        Text(
            "SpiMp3 · version ${com.spimp3.app.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun LegalRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = accentColor(), modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Rounded.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SecurityPoint(title: String, body: String) {
    Row(Modifier.padding(bottom = 12.dp)) {
        Icon(
            Icons.Rounded.Check,
            null,
            tint = accentColor(),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionRow(
    name: String,
    purpose: String,
    allowed: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            if (allowed) "•" else "✕",
            style = MaterialTheme.typography.bodySmall,
            color = if (allowed) accentColor() else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "— $purpose",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor().copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accentColor(), modifier = Modifier.size(20.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}
