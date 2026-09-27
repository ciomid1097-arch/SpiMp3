package com.spimp3.app.ui.screens

import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.spimp3.app.MainViewModel
import com.spimp3.app.data.Song
import com.spimp3.app.data.TagEditor
import com.spimp3.app.ui.theme.accentColor
import com.spimp3.app.ui.theme.onAccentColor
import kotlinx.coroutines.launch

/**
 * Edit the tags of an audio file: title, artist, album, year, track, genre and
 * embedded artwork. Saving asks for the system write-consent dialog first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongScreen(
    vm: MainViewModel,
    songId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val library by vm.library.collectAsState()
    val song = library.songById[songId]
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var tags by remember { mutableStateOf(TagEditor.Tags()) }
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var artworkBytes by remember { mutableStateOf<ByteArray?>(null) }
    var artworkPreview by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingSave by remember { mutableStateOf(false) }

    LaunchedEffect(songId) {
        song?.let {
            tags = TagEditor.read(it)
            loaded = true
        }
    }

    // System write-consent dialog (Android 11+), then we do the actual write.
    val writeConsent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (pendingSave) {
            pendingSave = false
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                saving = true
                scope.launch {
                    song?.let { s ->
                        val outcome = TagEditor.write(context, s, tags, artworkBytes)
                        TagEditor.rescan(context, s.data)
                        saving = false
                        if (outcome is TagEditor.WriteOutcome.Success) {
                            vm.refreshPermission()
                            scope.launch {
                                snackbar.showSnackbar("Tags updated")
                            }
                            onBack()
                        } else {
                            scope.launch {
                                snackbar.showSnackbar(
                                    (outcome as TagEditor.WriteOutcome.Failure).message,
                                )
                            }
                        }
                    }
                }
            } else {
                saving = false
            }
        }
    }

    val pickArtwork = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            artworkPreview = uri
            artworkBytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
        }
    }

    if (song == null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Song not found", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    fun save() {
        val sender = TagEditor.createWriteRequest(context, song.contentUri)
        if (sender != null) {
            pendingSave = true
            runCatching { writeConsent.launch(IntentSenderRequest.Builder(sender).build()) }
                .onFailure { pendingSave = false }
        } else {
            saving = true
            scope.launch {
                val outcome = TagEditor.write(context, song, tags, artworkBytes)
                TagEditor.rescan(context, song.data)
                saving = false
                if (outcome is TagEditor.WriteOutcome.Success) {
                    vm.refreshPermission()
                    onBack()
                } else {
                    snackbar.showSnackbar((outcome as TagEditor.WriteOutcome.Failure).message)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text("Edit details", style = MaterialTheme.typography.headlineSmall)
            }

            // Artwork
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (artworkPreview != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(artworkPreview).build(),
                        contentDescription = "New artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(com.spimp3.app.data.MusicRepository.albumArtUri(song.albumId))
                            .build(),
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    pickArtwork.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Rounded.PhotoLibrary, null)
                Text(
                    if (artworkPreview != null) "Change cover again" else "Change cover",
                    Modifier.padding(start = 8.dp),
                )
            }
            if (artworkBytes != null) {
                Text(
                    "New cover will be embedded in the file",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor(),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 6.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Field("Title", tags.title) { tags = tags.copy(title = it) }
            Field("Artist", tags.artist) { tags = tags.copy(artist = it) }
            Field("Album", tags.album) { tags = tags.copy(album = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    Field("Year", tags.year, numeric = true) { tags = tags.copy(year = it) }
                }
                Box(Modifier.weight(1f)) {
                    Field("Track", tags.track, numeric = true) { tags = tags.copy(track = it) }
                }
            }
            Field("Genre", tags.genre) { tags = tags.copy(genre = it) }

            Spacer(Modifier.height(10.dp))
            Text(
                "Changes are written into the audio file itself, not just the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { save() },
                enabled = !saving && loaded,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor(),
                    contentColor = onAccentColor(),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = onAccentColor(),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.Check, null)
                    Text("Save changes", Modifier.padding(start = 8.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp)) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    numeric: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    )
}
