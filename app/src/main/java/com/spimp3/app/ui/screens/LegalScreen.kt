package com.spimp3.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spimp3.app.ui.theme.accentColor

/** Which bundled legal document to show. */
enum class LegalDoc { PRIVACY_POLICY, TERMS }

private const val APP_VERSION = "1.0.0"
private const val LAST_UPDATED = "27 September 2026"

/**
 * In-app copy of the privacy policy and terms.
 *
 * Google Play expects a privacy policy to be reachable from inside the app as
 * well as hosted on a public URL, so both texts are kept here in plain Kotlin
 * (no network needed) and mirrored in store/privacy-policy.html.
 */
@Composable
fun LegalScreen(doc: LegalDoc, onBack: () -> Unit) {
    val scroll = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
            }
            Text(
                if (doc == LegalDoc.PRIVACY_POLICY) "Privacy policy" else "Terms of use",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 22.dp)
                .padding(bottom = 48.dp),
        ) {
            Text(
                "SpiMp3 · version $APP_VERSION",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor(),
            )
            Text(
                "Last updated: $LAST_UPDATED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            if (doc == LegalDoc.PRIVACY_POLICY) PrivacyPolicyBody() else TermsBody()
        }
    }
}

@Composable
private fun PrivacyPolicyBody() {
    H1("The short version")
    P(
        "SpiMp3 collects nothing, sends nothing and shares nothing. The app does " +
            "not hold the INTERNET permission, so it is technically incapable of " +
            "opening a network connection of any kind. Your music stays on your " +
            "phone, on your terms.",
    )

    H1("1. What we collect")
    P("Nothing. We do not collect personal information, usage data, diagnostics or crash reports. There is no analytics SDK, no advertising SDK and no telemetry of any kind in the app.")

    H1("2. What the app stores on your device")
    P("The following data is written only to the app's own private storage, which Android isolates from every other app. It never leaves the device and we cannot see it:")
    Bullet("Your settings — theme, accent colour, playback preferences")
    Bullet("Your playlists, favourites and recently played history")
    Bullet("The last playing queue and playback position, so playback resumes where you left off")
    P("You can erase all of this at any time by clearing SpiMp3's storage in Android Settings → Apps → SpiMp3 → Storage, or by uninstalling the app.")

    H1("3. Permissions, and why we ask for them")
    P("SpiMp3 requests only the following permissions. You can verify this list yourself in Android Settings → Apps → SpiMp3 → Permissions:")
    Bullet("Audio — required to find and play the music files already on your device.")
    Bullet("Notifications — required to show playback controls. Android shows a \"nearby devices\" style permission dialog; declining it only hides the notification.")
    Bullet("Foreground service — required so playback continues while the app is in the background.")
    Bullet("Wake lock — required so playback is not interrupted when the screen turns off.")
    P("SpiMp3 does not request the INTERNET permission. It also does not request access to your contacts, location, camera, microphone, files outside your music, or your device identifiers. Any \"network state\" capability that appears alongside the playback engine is inherited from Android's media framework, is read-only, and cannot transfer data.")

    H1("4. Your music files")
    P("SpiMp3 reads audio files in place, where they already live in Android's shared storage. It does not duplicate, move, convert, upload or modify them. The only exception is the \"Edit details\" action, which you must start yourself: it writes the title, artist, album, year, genre and cover art you type directly into the audio file's own metadata tags, on your device. Deleting a song always goes through Android's own confirmation dialog, and the operating system remains in control of the operation.")

    H1("5. Third parties")
    P("None. SpiMp3 contains no third-party advertising, analytics, attribution or social SDKs, and it has no backend servers of its own.")

    H1("6. Children")
    P("SpiMp3 is suitable for all ages and collects no data from anyone, including children.")

    H1("7. Your rights")
    P("Because no data is collected or transmitted, there is nothing for us to export, correct or delete on your behalf. Everything SpiMp3 stores is on your device and under your control through Android's own settings.")

    H1("8. Changes to this policy")
    P("If this policy changes, the updated version will ship with the app update and the date at the top of this page will change. Because the app has no network access, it cannot silently change anything about your data.")

    H1("9. Contact")
    P("Questions about this policy are welcome at workspikestudio@gmail.com, or on Telegram at @spike_c.")
}

@Composable
private fun TermsBody() {
    H1("1. Acceptance")
    P("By installing or using SpiMp3 you agree to these terms. If you do not agree, please uninstall the app.")

    H1("2. Licence")
    P("SpiMp3 is licensed, not sold. You may use it on any device running Android 8.0 (API 26) or later, for personal or internal use. Reverse engineering the app for the purpose of redistributing it is not permitted.")

    H1("3. Your content")
    P("SpiMp3 is a player for music you already own or have the right to play. It does not download, stream or provide music. You are responsible for making sure you have the necessary rights for the files on your device and for how you use them.")

    H1("4. Acceptable use")
    P("Do not use SpiMp3 to infringe copyright or any other right of a third party. Do not attempt to circumvent Android's security model, and do not use the app in a way that interferes with other applications or the device's stability.")

    H1("5. No warranty")
    P("The app is provided \"as is\", without warranty of any kind, express or implied, including fitness for a particular purpose. While we test carefully, we cannot guarantee uninterrupted playback, and audio quality depends on the source files and your device's output hardware.")

    H1("6. Limitation of liability")
    P("To the maximum extent permitted by law, the developer is not liable for any loss of data, lost profits or indirect damages arising from the use of the app. The app never deletes your music without an explicit action and Android's own confirmation dialog, but you remain responsible for keeping your own backups of important files.")

    H1("7. Changes")
    P("We may update the app and these terms from time to time. Continued use of an updated version constitutes acceptance of the updated terms.")

    H1("8. Contact")
    P("Questions about these terms: workspikestudio@gmail.com, or Telegram @spike_c.")
}

// ---------- tiny document primitives ----------

@Composable
private fun H1(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = accentColor(),
        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
    )
}

@Composable
private fun P(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Box(
            Modifier
                .padding(top = 7.dp, end = 10.dp)
                .size(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accentColor()),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
