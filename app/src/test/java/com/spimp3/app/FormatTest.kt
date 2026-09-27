package com.spimp3.app

import com.spimp3.app.data.formatDuration
import com.spimp3.app.data.formatSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatTest {

    @Test
    fun `formats durations`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:59", formatDuration(59_000))
        assertEquals("3:25", formatDuration(205_000))
        assertEquals("1:03:05", formatDuration(3_785_000))
    }

    @Test
    fun `formats sizes`() {
        assertEquals("512 B", formatSize(512))
        assertEquals("1.5 KB", formatSize(1_536))
        assertEquals("2.0 MB", formatSize(2 * 1_048_576))
    }
}
