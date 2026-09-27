package com.spimp3.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Single source of truth for the on-device music library.
 * Scans MediaStore off the main thread and exposes immutable snapshots.
 */
class MusicRepository(private val context: Context) {

    data class Library(
        val songs: List<Song> = emptyList(),
        val albums: List<Album> = emptyList(),
        val artists: List<Artist> = emptyList(),
        val folders: List<MusicFolder> = emptyList(),
        val loading: Boolean = false,
        val permissionGranted: Boolean = false,
    ) {
        val songById: Map<Long, Song> by lazy { songs.associateBy { it.id } }
        val albumById: Map<Long, Album> by lazy { albums.associateBy { it.id } }
        val artistById: Map<Long, Artist> by lazy { artists.associateBy { it.id } }
        val songsByAlbum: Map<Long, List<Song>> by lazy { songs.groupBy { it.albumId } }
        val songsByArtist: Map<Long, List<Song>> by lazy { songs.groupBy { it.artistId } }
        val songsByFolder: Map<String, List<Song>> by lazy { songs.groupBy { it.folder } }
    }

    private val _library = MutableStateFlow(Library())
    val library: StateFlow<Library> = _library.asStateFlow()

    suspend fun refresh(permissionGranted: Boolean) {
        if (!permissionGranted) {
            _library.value = Library(permissionGranted = false)
            return
        }
        _library.value = _library.value.copy(loading = true, permissionGranted = true)
        val songs = withContext(Dispatchers.IO) { scan() }
        val albums = songs.groupBy { it.albumId }.map { (id, list) ->
            val rep = list.sortedBy { it.track ?: 0 }.first()
            Album(
                id = id,
                title = rep.album,
                artist = rep.artist,
                year = rep.year,
                trackCount = list.size,
                durationMs = list.sumOf { it.durationMs },
            )
        }.sortedBy { it.title.lowercase() }
        val artists = songs.groupBy { it.artistId }.map { (id, list) ->
            val rep = list.first()
            Artist(
                id = id,
                name = rep.artist,
                albumCount = list.map { it.albumId }.distinct().size,
                trackCount = list.size,
            )
        }.sortedBy { it.name.lowercase() }
        val folders = songs.groupBy { it.folder }.map { (path, list) ->
            MusicFolder(path = path, name = path.substringAfterLast('/'), trackCount = list.size)
        }.sortedBy { it.name.lowercase() }
        _library.value = Library(
            songs = songs,
            albums = albums,
            artists = artists,
            folders = folders,
            loading = false,
            permissionGranted = true,
        )
    }

    private fun scan(): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 15000"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iArtistId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val iDuration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val iYear = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val iDateAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (c.moveToNext()) {
                songs += Song(
                    id = c.getLong(iId),
                    title = c.getString(iTitle) ?: "Unknown",
                    artist = (c.getString(iArtist) ?: "Unknown Artist").ifBlank { "Unknown Artist" },
                    album = (c.getString(iAlbum) ?: "Unknown Album").ifBlank { "Unknown Album" },
                    albumId = c.getLong(iAlbumId),
                    artistId = c.getLong(iArtistId),
                    durationMs = c.getLong(iDuration),
                    sizeBytes = c.getLong(iSize),
                    year = c.getInt(iYear).takeIf { it > 0 },
                    track = c.getInt(iTrack) % 1000,
                    data = c.getString(iData) ?: "",
                    dateAdded = c.getLong(iDateAdded),
                )
            }
        }
        return songs
    }

    companion object {
        fun albumArtUri(albumId: Long): Uri =
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

        fun artworkUriFor(albumId: Long): Uri = albumArtUri(albumId)
    }
}
