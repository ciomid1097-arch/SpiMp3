package com.spimp3.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spimp3.app.MainViewModel
import com.spimp3.app.ui.theme.Accents
import com.spimp3.app.ui.theme.ThemeMode
import com.spimp3.app.ui.theme.accentColor

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenLegal: (LegalDoc) -> Unit = {},
    debugScrollToBottom: Boolean = false,
) {
    val settings by vm.settings.collectAsState()
    val library by vm.library.collectAsState()
    val scrollState = rememberScrollState()

    // Debug-only: lets tooling inspect the bottom of the page without tapping.
    LaunchedEffect(debugScrollToBottom, library.songs.size) {
        if (debugScrollToBottom) {
            delay(400)
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        Row(
            Modifier.statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }

        SettingsGroup("Appearance")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeOption(
                    mode = mode,
                    selected = ThemeMode.from(settings.themeMode) == mode,
                    onClick = { vm.setThemeMode(mode.key) },
                )
            }
        }
        Text(
            if (ThemeMode.from(settings.themeMode) == ThemeMode.LIGHT) {
                "Light mode uses a pure white background."
            } else {
                "Dark mode uses pure black, which saves battery on OLED screens."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        SettingsGroup("Accent color")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Accents.all.forEach { (key, color) ->
                val selected = key == settings.accent
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 3.dp else 0.dp,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable { vm.setAccent(key) },
                )
            }
        }

        SettingsGroup("Playback")
        ToggleRow("Gapless playback", settings.gapless) { vm.setGapless(it) }

        SettingsGroup("Library")
        InfoRow("Tracks", "${library.songs.size}")
        InfoRow("Albums", "${library.albums.size}")
        InfoRow("Artists", "${library.artists.size}")

        SettingsGroup("About")
        InfoRow("App", "SpiMp3 1.0.0")
        InfoRow("Player", "ExoPlayer (Media3)")
        Text(
            "A beautiful, fast, fully offline music player.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
        )

        // Security & privacy + developer contact, pinned to the very bottom
        SecurityAndContactSection(onOpenLegal = onOpenLegal)
    }
}

@Composable
private fun SettingsGroup(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = accentColor(),
        modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp),
    )
}

/** Dark / Light / System picker with a tiny live preview swatch. */
@Composable
private fun ThemeOption(mode: ThemeMode, selected: Boolean, onClick: () -> Unit) {
    val accent = accentColor()
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) accent.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Preview: a bar of the background colour with an accent dot on it.
        Box(
            Modifier
                .size(width = 46.dp, height = 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (mode) {
                        ThemeMode.DARK -> Color(0xFF000000)
                        ThemeMode.LIGHT -> Color(0xFFFFFFFF)
                        ThemeMode.SYSTEM -> Color(0xFF7A7A82)
                    }
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Text(
            mode.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
