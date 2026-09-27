package com.spimp3.app.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * Deletes an audio file, always with the user's explicit consent.
 *
 * On Android 11+ an app can never delete someone else's media silently, so we
 * ask MediaStore to show its own confirmation dialog. Below that we delete
 * through the ContentResolver, which is allowed for the user's own media.
 */
object SongDeleter {

    sealed interface Result {
        data object Deleted : Result
        data object Cancelled : Result
        data class Failed(val message: String) : Result
    }

    fun needsConsent(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun createDeleteRequest(context: Context, uri: Uri): android.app.PendingIntent? =
        createDeleteRequest(context, listOf(uri))

    /** Bulk variant: Android shows one dialog for the whole selection. */
    fun createDeleteRequest(context: Context, uris: List<Uri>): android.app.PendingIntent? {
        if (!needsConsent() || uris.isEmpty()) return null
        return MediaStore.createDeleteRequest(context.contentResolver, uris)
    }

    /** Direct delete for API < 30. Returns true when a row was removed. */
    fun deleteDirect(context: Context, uri: Uri): Result {
        return runCatching {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) Result.Deleted else Result.Failed("Android refused to delete this file")
        }.getOrElse { Result.Failed(it.message ?: "Delete failed") }
    }
}
