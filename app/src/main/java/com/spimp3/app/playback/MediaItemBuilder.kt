package com.spimp3.app.playback

import android.content.Context
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.spimp3.app.data.MusicRepository

/**
 * Builds Media3 [MediaItem]s from MediaStore song ids, with full metadata so the
 * notification and lock screen show title, artist and album art.
 */
object MediaItemBuilder {

    fun buildMany(context: Context, songIds: List<Long>): List<MediaItem> {
        if (songIds.isEmpty()) return emptyList()
        val map = querySongs(context, songIds)
        return songIds.mapNotNull { id ->
            map[id]?.let { (title, artist, album, albumId) ->
                build(title, artist, album, albumId, id)
            }
        }
    }

    fun build(context: Context, songId: Long): MediaItem? {
        val meta = querySongs(context, listOf(songId))[songId] ?: return null
        return build(meta.title, meta.artist, meta.album, meta.albumId, songId)
    }

    private fun build(
        title: String,
        artist: String,
        album: String,
        albumId: Long,
        songId: Long,
    ): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
        if (albumId > 0) {
            metadataBuilder.setArtworkUri(MusicRepository.albumArtUri(albumId))
        }
        return MediaItem.Builder()
            .setMediaId(songId.toString())
            .setUri("content://media/external/audio/media/$songId")
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun querySongs(
        context: Context,
        ids: List<Long>,
    ): Map<Long, SongMeta> {
        val out = mutableMapOf<Long, SongMeta>()
        if (ids.isEmpty()) return out
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
        )
        // Chunk the IN clause to stay well under SQLite variable limits.
        ids.chunked(400).forEach { chunk ->
            val idList = chunk.joinToString(",")
            runCatching {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Audio.Media._ID} IN ($idList)",
                    null,
                    null,
                )?.use { c ->
                    val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    while (c.moveToNext()) {
                        out[c.getLong(iId)] = SongMeta(
                            title = c.getString(iTitle) ?: "Unknown",
                            artist = (c.getString(iArtist) ?: "Unknown Artist").ifBlank { "Unknown Artist" },
                            album = (c.getString(iAlbum) ?: "Unknown Album").ifBlank { "Unknown Album" },
                            albumId = c.getLong(iAlbumId),
                        )
                    }
                }
            }
        }
        return out
    }

    private data class SongMeta(
        val title: String,
        val artist: String,
        val album: String,
        val albumId: Long,
    )
}
