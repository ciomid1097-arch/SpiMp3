package com.spimp3.app

import com.spimp3.app.data.SettingsStore
import com.spimp3.app.data.Song
import com.spimp3.app.ui.screens.sortSongs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SortOrderTest {

    private fun song(id: Long, title: String, artist: String = "A", album: String = "X", added: Long = 0, dur: Long = 1000) =
        Song(
            id = id, title = title, artist = artist, album = album,
            albumId = 1, artistId = 1, durationMs = dur, sizeBytes = 0,
            year = null, track = null, data = "/x/$title.mp3", dateAdded = added,
        )

    @Test
    fun `sort by title is case insensitive`() {
        val songs = listOf(song(1, "banana"), song(2, "Apple"), song(3, "cherry"))
        val sorted = sortSongs(songs, SettingsStore.SortOrder.TITLE)
        assertEquals(listOf("Apple", "banana", "cherry"), sorted.map { it.title })
    }

    @Test
    fun `sort by artist and album`() {
        val songs = listOf(song(1, "t1", artist = "Zed"), song(2, "t2", artist = "Alpha"))
        assertEquals(listOf("t2", "t1"), sortSongs(songs, SettingsStore.SortOrder.ARTIST).map { it.title })
        val byAlbum = listOf(song(1, "t1", album = "B"), song(2, "t2", album = "A"))
        assertEquals(listOf("t2", "t1"), sortSongs(byAlbum, SettingsStore.SortOrder.ALBUM).map { it.title })
    }

    @Test
    fun `sort by recent and duration`() {
        val songs = listOf(song(1, "old", added = 100), song(2, "new", added = 300), song(3, "mid", added = 200))
        assertEquals(listOf("new", "mid", "old"), sortSongs(songs, SettingsStore.SortOrder.RECENT).map { it.title })
        val durs = listOf(song(1, "short", dur = 1000), song(2, "long", dur = 999_000))
        assertEquals(listOf("long", "short"), sortSongs(durs, SettingsStore.SortOrder.DURATION).map { it.title })
    }
}
