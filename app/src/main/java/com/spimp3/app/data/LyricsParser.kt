package com.spimp3.app.data

import java.io.File

/**
 * Minimal .lrc (line-based lyrics) parser.
 * Format: [mm:ss.xx] line text — multiple timestamps per line supported.
 * Returns null when the file is missing or contains no timed lines.
 */
object LyricsParser {

    data class LyricLine(val timeMs: Long, val text: String)

    private val TIME_REGEX = Regex("""\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    fun parse(file: File?): List<LyricLine>? {
        if (file == null || !file.exists()) return null
        val lines = runCatching { file.readLines(Charsets.UTF_8) }.getOrNull() ?: return null
        return parse(lines)
    }

    fun parse(content: List<String>): List<LyricLine>? {
        val out = mutableListOf<LyricLine>()
        for (raw in content) {
            val matches = TIME_REGEX.findAll(raw).toList()
            if (matches.isEmpty()) continue
            val text = raw.substringAfterLast(']').trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val fracRaw = m.groupValues[3]
                val fracMs = when (fracRaw.length) {
                    0 -> 0L
                    1 -> fracRaw.toLong() * 100
                    2 -> fracRaw.toLong() * 10
                    else -> fracRaw.take(3).toLong()
                }
                out += LyricLine(min * 60_000 + sec * 1000 + fracMs, text)
            }
        }
        if (out.isEmpty()) return null
        return out.sortedBy { it.timeMs }
    }

    /** Find the .lrc file that belongs to an audio file path. */
    fun lrcFileFor(audioPath: String): File? {
        val f = File(audioPath)
        val base = f.absolutePath.removeSuffix(".${f.extension}")
        val candidates = listOf("$base.lrc", "$base.LRC")
        for (c in candidates) {
            val file = File(c)
            if (file.exists()) return file
        }
        // also look in sibling "lyrics" folder
        val alt = File(f.parentFile, "lyrics/${f.nameWithoutExtension}.lrc")
        return if (alt.exists()) alt else null
    }

    /** Index of the active line for playback position, or -1 before the first stamp. */
    fun activeLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var lo = 0
        var hi = lines.size - 1
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (lines[mid].timeMs <= positionMs) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return ans
    }
}
