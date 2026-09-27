package com.spimp3.app

import com.spimp3.app.data.LyricsParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LyricsParserTest {

    @Test
    fun `parses simple lrc with multiple timestamps`() {
        val lines = listOf(
            "[00:12.00]First line",
            "[00:17.20][01:20.05]Repeated line",
            "not a timed line",
            "[00:25]Third line",
        )
        val parsed = LyricsParser.parse(lines)!!
        // 4 timestamps total, sorted by time
        assertEquals(4, parsed.size)
        assertEquals(12_000L, parsed[0].timeMs)
        assertEquals("First line", parsed[0].text)
        assertEquals(17_200L, parsed[1].timeMs)
        assertEquals(25_000L, parsed[2].timeMs)
        assertEquals(1 * 60_000 + 20_050L, parsed[3].timeMs)
    }

    @Test
    fun `returns null when no timed lines`() {
        assertNull(LyricsParser.parse(listOf("hello", "world")))
    }

    @Test
    fun `active line binary search finds current line`() {
        val lines = LyricsParser.parse(
            listOf("[00:10]A", "[00:20]B", "[00:30]C", "[00:40]D"),
        )!!
        assertEquals(-1, LyricsParser.activeLineIndex(lines, 5_000))
        assertEquals(0, LyricsParser.activeLineIndex(lines, 10_000))
        assertEquals(0, LyricsParser.activeLineIndex(lines, 19_999))
        assertEquals(1, LyricsParser.activeLineIndex(lines, 20_000))
        assertEquals(3, LyricsParser.activeLineIndex(lines, 99_000))
    }
}
