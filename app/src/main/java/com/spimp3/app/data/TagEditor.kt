package com.spimp3.app.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Reads and writes the tags (title/artist/album/artwork) of an audio file.
 *
 * Writing needs the user's consent on Android 11+:
 *  - API 30+: [MediaStore.createWriteRequest] returns a PendingIntent the app
 *    must launch; the system shows its own "allow editing?" dialog.
 *  - Below API 30 we write straight to the file path.
 *
 * After consent we write via the raw path when possible, and fall back to a
 * copy-in/copy-out through a ParcelFileDescriptor when scoped storage blocks
 * direct access.
 */
object TagEditor {

    data class Tags(
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val year: String = "",
        val track: String = "",
        val genre: String = "",
        val hasArtwork: Boolean = false,
    )

    sealed interface WriteOutcome {
        data object Success : WriteOutcome
        data class Failure(val message: String) : WriteOutcome
    }

    fun needsConsent(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** Consent intent for editing this file's tags, or null on older devices. */
    fun createWriteRequest(context: Context, uri: Uri): android.app.PendingIntent? {
        if (!needsConsent()) return null
        return MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
    }

    suspend fun read(song: Song): Tags = withContext(Dispatchers.IO) {
        val fallback = Tags(
            title = song.title,
            artist = song.artist,
            album = song.album,
            year = song.year?.toString() ?: "",
        )
        val file = File(song.data)
        if (!file.exists()) return@withContext fallback
        runCatching {
            val af: AudioFile = AudioFileIO.read(file)
            val tag = af.tagOrCreateAndSetDefault
            Tags(
                title = value(tag, FieldKey.TITLE) ?: song.title,
                artist = value(tag, FieldKey.ARTIST) ?: song.artist,
                album = value(tag, FieldKey.ALBUM) ?: song.album,
                year = value(tag, FieldKey.YEAR) ?: "",
                track = value(tag, FieldKey.TRACK) ?: "",
                genre = value(tag, FieldKey.GENRE) ?: "",
                hasArtwork = tag.firstArtwork != null,
            )
        }.getOrDefault(fallback)
    }

    /**
     * Writes the tags into the file. [artworkBytes] is optional; when null the
     * existing artwork is left untouched.
     */
    suspend fun write(
        context: Context,
        song: Song,
        tags: Tags,
        artworkBytes: ByteArray?,
    ): WriteOutcome = withContext(Dispatchers.IO) {
        val file = File(song.data)
        if (!file.exists()) return@withContext WriteOutcome.Failure("File not found: ${song.fileName}")

        // 1) Preferred: direct file access (pre-scoped-storage, and on Android
        //    11+ once the user has approved the write request).
        val direct = runCatching {
            val af = AudioFileIO.read(file)
            applyTags(af, tags, artworkBytes)
            af.commit()
            true
        }.getOrDefault(false)
        if (direct) return@withContext WriteOutcome.Success

        // 2) Fallback: copy in through the MediaStore URI, edit the temp copy,
        //    then copy the bytes back through a file descriptor.
        runCatching {
            val temp = File(context.cacheDir, "tagedit-in-${song.id}.tmp")
            context.contentResolver.openInputStream(song.contentUri)?.use { input ->
                temp.outputStream().use { input.copyTo(it) }
            } ?: return@runCatching false

            val af = AudioFileIO.read(temp)
            applyTags(af, tags, artworkBytes)

            val edited = File(context.cacheDir, "tagedit-out-${song.id}.tmp")
            af.file = edited
            af.commit()

            val ok = edited.inputStream().use { input ->
                context.contentResolver.openFileDescriptor(song.contentUri, "rw")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { out ->
                        input.copyTo(out)
                        out.flush()
                    }
                } != null
            }
            edited.delete()
            temp.delete()
            ok
        }.getOrElse { false }
            .let { ok -> if (ok) WriteOutcome.Success else WriteOutcome.Failure("Could not write tags") }
    }

    private fun applyTags(af: AudioFile, tags: Tags, artworkBytes: ByteArray?) {
        val tag = af.tagOrCreateAndSetDefault
        set(tag, FieldKey.TITLE, tags.title)
        set(tag, FieldKey.ARTIST, tags.artist)
        set(tag, FieldKey.ALBUM, tags.album)
        set(tag, FieldKey.YEAR, tags.year.trim().takeIf { it.isNotEmpty() })
        set(tag, FieldKey.TRACK, tags.track.trim().takeIf { it.isNotEmpty() })
        if (tags.genre.isNotBlank()) set(tag, FieldKey.GENRE, tags.genre)
        if (artworkBytes != null) {
            val art = ArtworkFactory.getNew()
            art.binaryData = artworkBytes
            art.mimeType = guessMime(artworkBytes)
            runCatching { tag.setField(art) }
        }
    }

    private fun set(tag: org.jaudiotagger.tag.Tag, key: FieldKey, value: String?) {
        runCatching {
            if (value.isNullOrBlank()) {
                tag.deleteField(key)
            } else {
                tag.setField(key, value)
            }
        }
    }

    private fun value(tag: org.jaudiotagger.tag.Tag, key: FieldKey): String? = runCatching {
        tag.getValue(key, 0)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun guessMime(bytes: ByteArray): String = when {
        bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "image/png"
        bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
        bytes.size > 12 && bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() -> "image/webp"
        else -> "image/jpeg"
    }

    /** Ask MediaStore to re-read the file so the UI shows the new tags. */
    fun rescan(context: Context, path: String) {
        runCatching {
            android.media.MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
        }
    }
}
