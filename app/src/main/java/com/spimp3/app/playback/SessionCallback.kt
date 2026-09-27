package com.spimp3.app.playback

import android.os.Bundle
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

interface SleepTimerControl {
    fun start(minutes: Int)
    fun endOfQueue()
    fun clear()
}

/**
 * Callback for the [MediaLibrarySession]. The base MediaSession.Callback methods
 * receive the base [MediaSession] type; library-specific methods receive the
 * [MediaLibrarySession] type.
 */
class SessionCallback(
    private val sleepTimerControl: SleepTimerControl,
) : MediaLibrarySession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            .add(SessionCommand(PlaybackService.CUSTOM_COMMAND_SET_SHUFFLE, Bundle.EMPTY))
            .add(SessionCommand(PlaybackService.CUSTOM_COMMAND_SLEEP_TIMER, Bundle.EMPTY))
            .build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(sessionCommands)
            .build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        return when (customCommand.customAction) {
            PlaybackService.CUSTOM_COMMAND_SET_SHUFFLE -> {
                session.player.shuffleModeEnabled = args.getBoolean("enable", false)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            PlaybackService.CUSTOM_COMMAND_SLEEP_TIMER -> {
                // minutes: -1 = clear, 0 = end of queue, n>0 = minutes
                when (val minutes = args.getInt("minutes", 0)) {
                    -1 -> sleepTimerControl.clear()
                    0 -> sleepTimerControl.endOfQueue()
                    else -> sleepTimerControl.start(minutes)
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            else -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }
}
