package com.spimp3.app.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer

/**
 * Session-scoped audio effects engine attached to the player's audio session.
 *
 * Lifecycle contract (deliberate):
 *  - [enabled] is **memory-only**. Nothing here touches disk for the toggle, so
 *    every cold app start boots with the EQ off; the user re-enables it and the
 *    last-used band values are still right there.
 *  - Presets (band levels + a name) ARE persisted by the caller through
 *    [PresetStore], so a saved "Bass" preset survives restarts.
 *  - Instances are re-created lazily: [attach] is called whenever the audio
 *    session id becomes known (ExoPlayer hands it out after prepare()), and
 *    [detach] releases the effects (called when playback stops or scope dies).
 */
class AudioFxController {
    companion object {
        /** UI works in ±15 dB like most players; audiofx reports milli-bel. */
        const val RANGE_DB = 15

        private val BAND_LABELS = arrayOf("60", "230", "910", "3.6k", "14k")
        private val PRESET_NAMES = arrayOf("Custom", "Pop", "Dance", "Bass", "Treble", "Bass & Treble")

        /** ±dB per band for each built-in preset (5 bands). */
        private val PRESET_GAINS = mapOf(
            "Pop" to floatArrayOf(-1f, 2f, 4f, 2f, -1f),
            "Dance" to floatArrayOf(5f, 3f, 1f, 0f, 2f),
            "Bass" to floatArrayOf(8f, 5f, 0f, -1f, -1f),
            "Treble" to floatArrayOf(-3f, -1f, 1f, 4f, 7f),
            "Bass & Treble" to floatArrayOf(6f, 3f, 0f, 3f, 6f),
        )

        fun presetNames(): List<String> = PRESET_NAMES.toList()
        fun bandLabel(index: Int): String = BAND_LABELS.getOrElse(index) { "?" }
    }

    data class Preset(
        val name: String,
        val bandDb: List<Float>,
        val bass: Int = 0, // 0..1000
        val treble: Int = 0, // 0..1000
    )

    var enabled: Boolean = false
        private set

    /** -1 until ExoPlayer's audio session is live. */
    var sessionId: Int = -1
        private set

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var treble: Virtualizer? = null

    // Last-known state so re-attach (new session id) restores the same sound.
    private var bandsDb = FloatArray(5)
    private var bassStrength = 0
    private var trebleStrength = 0

    val bandCount: Int get() = bandsDb.size

    /** Must run on a thread with a Looper; call from the main scope. */
    fun attach(sessionId: Int) {
        if (sessionId <= 0) return
        detach()
        this.sessionId = sessionId
        runCatching { equalizer = Equalizer(0, sessionId).also { it.enabled = true } }
            .onFailure { equalizer = null }
        runCatching { bassBoost = BassBoost(0, sessionId) }.onFailure { bassBoost = null }
        runCatching { treble = Virtualizer(0, sessionId) }.onFailure { treble = null }
        if (enabled) applyCurrent()
    }

    fun detach() {
        runCatching { equalizer?.enabled = false; equalizer?.release() }
        runCatching { bassBoost?.enabled = false; bassBoost?.release() }
        runCatching { treble?.enabled = false; treble?.release() }
        equalizer = null; bassBoost = null; treble = null
        sessionId = -1
    }

    /** Turns the whole effects chain on for this app session only (never persisted). */
    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            runCatching { equalizer?.enabled = false }
            runCatching { bassBoost?.setStrength(0.toShort()) }
            runCatching { treble?.setStrength(0.toShort()) }
        } else {
            applyCurrent()
        }
    }

    fun setBandDb(index: Int, db: Float) {
        if (index !in bandsDb.indices) return
        bandsDb[index] = db.coerceIn(-RANGE_DB.toFloat(), RANGE_DB.toFloat())
        if (enabled) runCatching {
            equalizer?.setBandLevel(index.toShort(), (db * 100).toInt().toShort())
        }
    }

    fun bands(): FloatArray = bandsDb.copyOf()

    fun setBands(db: List<Float>) {
        for (i in bandsDb.indices) setBandDb(i, db.getOrElse(i) { 0f })
    }

    fun setBass(strength: Int) { // 0..1000
        bassStrength = strength.coerceIn(0, 1000)
        if (enabled) runCatching { bassBoost?.setStrength(bassStrength.toShort()) }
    }

    fun setTreble(strength: Int) { // 0..1000
        trebleStrength = strength.coerceIn(0, 1000)
        if (enabled) runCatching { treble?.setStrength(trebleStrength.toShort()) }
    }

    fun bass(): Int = bassStrength
    fun treble(): Int = trebleStrength

    private fun applyCurrent() {
        runCatching {
            val eq = equalizer ?: return
            eq.enabled = true
            for (i in bandsDb.indices) {
                eq.setBandLevel(i.toShort(), (bandsDb[i] * 100).toInt().toShort())
            }
        }
        runCatching { bassBoost?.enabled = true; bassBoost?.setStrength(bassStrength.toShort()) }
        runCatching { treble?.enabled = true; treble?.setStrength(trebleStrength.toShort()) }
    }

    // ---- Presets (state only; persistence is the caller's job) ----

    fun currentPreset(): Preset = Preset(
        name = "Custom",
        bandDb = bandsDb.toList(),
        bass = bassStrength,
        treble = trebleStrength,
    )

    fun applyPreset(p: Preset) {
        setBands(p.bandDb)
        setBass(p.bass)
        setTreble(p.treble)
    }

    fun applyBuiltIn(name: String) {
        val g = PRESET_GAINS[name] ?: return
        setBands(g.toList())
        // Built-ins also shape the knobs, matching the reference app.
        setBass(if (name == "Bass" || name == "Bass & Treble") 600 else 0)
        setTreble(if (name == "Treble" || name == "Bass & Treble") 600 else 0)
    }
}
