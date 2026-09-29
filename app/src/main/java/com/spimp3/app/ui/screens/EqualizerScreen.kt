package com.spimp3.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spimp3.app.MainViewModel
import com.spimp3.app.audio.AudioFxController
import com.spimp3.app.ui.theme.accentColor
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Full equalizer screen modelled on the reference app:
 * back + title + overflow menu (Save Preset / Delete Preset / Settings),
 * Enable toggle, horizontal preset chips, a connected 5-band curve with
 * draggable handles, a volume slider and BASS / TREBLE knobs.
 */
@Composable
fun EqualizerScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
) {
    val fx = vm.audioFx
    val presets by vm.userFxPresets.collectAsState()
    val scope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(fx.enabled) }
    var bands by remember { mutableStateOf(fx.bands().toList()) }
    var bass by remember { mutableIntStateOf(fx.bass()) }
    var treble by remember { mutableIntStateOf(fx.treble()) }
    var selectedPreset by remember { mutableStateOf("Custom") }
    var menuOpen by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val volume = vm.deviceVolume
    val volumeFloat by volume.flow.collectAsState()

    fun refreshFromFx() {
        bands = fx.bands().toList()
        bass = fx.bass()
        treble = fx.treble()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        // ---- Header ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Equalizer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Save Preset") },
                        leadingIcon = { Icon(Icons.Rounded.Save, null) },
                        onClick = { menuOpen = false; showSave = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Preset") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = { menuOpen = false; showDelete = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                        onClick = { menuOpen = false; showSettings = true },
                    )
                }
            }
        }

        // ---- Enable row ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enable", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    fx.setEnabled(it)
                    refreshFromFx()
                },
            )
        }

        // ---- Preset chips ----
        val chipNames = buildList {
            addAll(AudioFxController.presetNames())
            presets.forEach { p -> add(p.name) }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            chipNames.forEach { name ->
                val selected = selectedPreset == name
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            selectedPreset = name
                            val user = presets.firstOrNull { it.name == name }
                            if (user != null) {
                                fx.applyPreset(
                                    AudioFxController.Preset(
                                        name = user.name,
                                        bandDb = user.bands,
                                        bass = user.bass,
                                        treble = user.treble,
                                    ),
                                )
                            } else {
                                fx.applyBuiltIn(name)
                            }
                            refreshFromFx()
                        }
                        .padding(vertical = 6.dp),
                )
            }
        }

        // ---- 5-band connected curve ----
        BandCurve(
            bands = bands,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 320.dp)
                .padding(vertical = 18.dp),
            onChange = { index, db ->
                bands = bands.toMutableList().also { it[index] = db }
                selectedPreset = "Custom"
                fx.setBandDb(index, db)
            },
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text("Low", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Text("Mid", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Text("High", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }

        // ---- Volume ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Volume", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(
                "${(volumeFloat * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = accentColor(),
            )
        }
        Slider(
            value = volumeFloat,
            onValueChange = { volume.set(it) },
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = accentColor(),
                activeTrackColor = accentColor(),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
            modifier = Modifier.padding(bottom = 14.dp),
        )

        // ---- BASS / TREBLE knobs ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 26.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Knob(
                label = "BASS",
                value = bass / 1000f,
                onChange = {
                    bass = (it * 1000).roundToInt()
                    fx.setBass(bass)
                    selectedPreset = "Custom"
                },
            )
            Knob(
                label = "TREBLE",
                value = treble / 1000f,
                onChange = {
                    treble = (it * 1000).roundToInt()
                    fx.setTreble(treble)
                    selectedPreset = "Custom"
                },
            )
        }
    }

    // ---- Save Preset dialog ----
    if (showSave) {
        var name by remember { mutableStateOf(selectedPreset.takeIf { it != "Custom" } ?: "") }
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("Save Preset") },
            text = {
                Column {
                    Text("Name your current equalizer settings.")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = name.trim().ifBlank { "Preset" }
                        scope.launch {
                            vm.saveFxPreset(
                                com.spimp3.app.audio.PresetStore.StoredPreset(
                                    name = clean,
                                    bands = bands,
                                    bass = bass,
                                    treble = treble,
                                ),
                            )
                        }
                        selectedPreset = clean
                        showSave = false
                    },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("Cancel") } },
        )
    }

    // ---- Delete Preset dialog ----
    if (showDelete) {
        if (presets.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showDelete = false },
                title = { Text("Delete Preset") },
                text = { Text("No saved presets yet.") },
                confirmButton = {
                    TextButton(onClick = { showDelete = false }) { Text("OK") }
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = { showDelete = false },
                title = { Text("Delete Preset") },
                text = {
                    Column {
                        presets.forEach { p ->
                            Text(
                                p.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { vm.deleteFxPreset(p.name) }
                                        if (selectedPreset == p.name) selectedPreset = "Custom"
                                        showDelete = false
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
            )
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Audio Effects") },
            text = {
                Text(
                    "The equalizer applies to everything SpiMp3 plays. " +
                        "It stays on until you close the app; saved presets remain available " +
                        "so you can re-enable them any time.",
                )
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("OK") } },
        )
    }
}

/** Connected draggable 5-band curve with vertical rails and round handles. */
@Composable
private fun BandCurve(
    bands: List<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Int, Float) -> Unit,
) {
    val accent = accentColor()
    var dragging by remember { mutableStateOf(false) }

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(bands.size) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val slot = w / bands.size
                        var idx = (down.position.x / slot).toInt().coerceIn(0, bands.size - 1)
                        dragging = true
                        onChange(idx, yToDb(down.position.y, h))
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            idx = (change.position.x / slot).toInt().coerceIn(0, bands.size - 1)
                            onChange(idx, yToDb(change.position.y, h))
                            change.consume()
                        }
                        dragging = false
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val railX = bands.indices.map { (it + 0.5f) * w / bands.size }

            // Rails
            for (x in railX) {
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round,
                )
            }
            // Active part of each rail up to the handle
            val ys = bands.map { dbToY(it, h) }
            for (i in bands.indices) {
                if (enabled) {
                    drawLine(
                        color = accent.copy(alpha = 0.75f),
                        start = Offset(railX[i], ys[i]),
                        end = Offset(railX[i], h),
                        strokeWidth = 10f,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(
                    color = if (enabled) accent else Color.White.copy(alpha = 0.35f),
                    radius = 26f,
                    center = Offset(railX[i], ys[i]),
                )
                drawCircle(
                    color = Color.White,
                    radius = 11f,
                    center = Offset(railX[i], ys[i]),
                )
            }
            // Connected smooth curve through the handles
            if (bands.size >= 2) {
                val path = Path()
                path.moveTo(railX[0], ys[0])
                for (i in 0 until bands.size - 1) {
                    val midX = (railX[i] + railX[i + 1]) / 2f
                    path.cubicTo(midX, ys[i], midX, ys[i + 1], railX[i + 1], ys[i + 1])
                }
                drawPath(
                    path,
                    color = if (enabled) accent else Color.White.copy(alpha = 0.30f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f),
                )
            }
        }
    }
}

private fun dbToY(db: Float, h: Float): Float = h / 2f - (db / 15f) * (h / 2f - 30f)

private fun yToDb(y: Float, h: Float): Float =
    ((h / 2f - y) / (h / 2f - 30f) * 15f).coerceIn(-15f, 15f)

/** Circular BASS/TREBLE knob: drag around the ring to set the value. */
@Composable
private fun Knob(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    val accent = accentColor()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .size(110.dp)
                .pointerInput(Unit) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val pos = change.position
                            val angle = Math.toDegrees(
                                kotlin.math.atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble()),
                            )
                            // Map to 0..270 degrees, starting bottom-left (-225..45)
                            val mapped = (((angle + 225.0 + 360.0) % 360.0) / 270.0).coerceIn(0.0, 1.0)
                            onChange(mapped.toFloat())
                            change.consume()
                        }
                    }
                },
        ) {
            val stroke = 20f
            val inset = stroke
            val sweep = 270f
            val start = 135f
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = start,
                sweepAngle = sweep * value.coerceIn(0f, 1f),
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Indicator dot at the current angle
            val ang = Math.toRadians((start + sweep * value.coerceIn(0f, 1f)).toDouble())
            val r = (size.minDimension - stroke * 2) / 2f
            drawCircle(
                color = Color.White,
                radius = 9f,
                center = Offset(
                    this.center.x + (r * cos(ang)).toFloat(),
                    this.center.y + (r * sin(ang)).toFloat(),
                ),
            )
        }
        Text(
            label,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}