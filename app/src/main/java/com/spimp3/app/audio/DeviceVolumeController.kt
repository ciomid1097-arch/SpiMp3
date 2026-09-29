package com.spimp3.app.audio

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI-friendly wrapper around [AudioManager] for the Equalizer screen's volume
 * slider (the reference app drives the device media volume from there).
 */
class DeviceVolumeController(private val context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _flow = MutableStateFlow(normalised())
    val flow: StateFlow<Float> = _flow.asStateFlow()

    private fun max() = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    private fun normalised(): Float =
        audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max()

    fun set(v: Float) {
        val clamped = v.coerceIn(0f, 1f)
        val target = (clamped * max()).roundToIntCompat()
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
        _flow.value = clamped
    }

    fun close() = scope.cancel()
}

private fun Float.roundToIntCompat(): Int = (this + 0.5f).toInt()
