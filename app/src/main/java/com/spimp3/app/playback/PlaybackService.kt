package com.spimp3.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.spimp3.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground playback service: ExoPlayer + MediaLibraryService.
 * - Notification, media button routing and audio focus handled by Media3
 * - Custom session commands: SET_SHUFFLE / SLEEP_TIMER (minutes: -1 clear, 0 end-of-queue, n>0 minutes)
 * - Becoming noisy (headphones unplugged) pauses playback
 * - Restores last queue & position after process death
 */
class PlaybackService : MediaLibraryService() {

    companion object {
        const val CUSTOM_COMMAND_SET_SHUFFLE = "com.spimp3.app.SET_SHUFFLE"
        const val CUSTOM_COMMAND_SLEEP_TIMER = "com.spimp3.app.SLEEP_TIMER"
        const val ACTION_SSM = "com.spimp3.app.action.SSM"
        const val EXTRA_MINUTES = "minutes"
        const val PREFS = "spimp3_playback"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var player: ExoPlayer? = null
    private var session: MediaLibrarySession? = null
    private var sleepTimer: SleepTimer? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull() ?: return
            scope.launch { com.spimp3.app.data.SettingsStore(applicationContext).pushRecent(id) }
            saveState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val timer = sleepTimer ?: return
            val p = player ?: return
            if (timer.endOfQueue &&
                playbackState == Player.STATE_ENDED &&
                p.mediaItemCount > 0 &&
                p.currentMediaItemIndex == p.mediaItemCount - 1
            ) {
                p.pause()
                clearSleepTimer()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(30_000, 120_000, 1_000, 3_000)
                    .build(),
            )
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(listener)
        this.player = player

        setMediaNotificationProvider(createNotificationProvider(this))

        session = MediaLibrarySession.Builder(
            this,
            player,
            SessionCallback(
                sleepTimerControl = object : SleepTimerControl {
                    override fun start(minutes: Int) =
                        startSleepTimer(minutes * 60_000L, endOfQueue = false)
                    override fun endOfQueue() = sleepEndOfQueue()
                    override fun clear() = clearSleepTimer()
                },
            ),
        )
            .setSessionActivity(sessionActivityIntent())
            .build()

        scope.launch { restoreLastQueue() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SSM) {
            when (val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)) {
                -1 -> clearSleepTimer()
                0 -> sleepEndOfQueue()
                else -> startSleepTimer(minutes * 60_000L, endOfQueue = false)
            }
            return START_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        saveState()
        clearSleepTimer()
        session?.release()
        session = null
        player?.release()
        player = null
        scope.cancel()
        super.onDestroy()
    }

    // ---- Sleep timer ----

    private fun sleepEndOfQueue() {
        clearSleepTimer()
        sleepTimer = SleepTimer(endOfQueue = true, fireAtElapsedRealtime = 0L)
    }

    private fun startSleepTimer(durationMs: Long, endOfQueue: Boolean) {
        clearSleepTimer()
        sleepTimer = SleepTimer(endOfQueue, SystemClock.elapsedRealtime() + durationMs).also {
            it.start(scope) {
                player?.pause()
                clearSleepTimer()
            }
        }
    }

    private fun clearSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
    }

    // ---- Queue persistence ----

    private fun saveState() {
        val p = player ?: return
        runCatching {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(
                    "media_ids",
                    (0 until p.mediaItemCount).joinToString(",") { i -> p.getMediaItemAt(i).mediaId },
                )
                .putInt("index", p.currentMediaItemIndex)
                .putLong("position", p.currentPosition.coerceAtLeast(0))
                .apply()
        }
    }

    private suspend fun restoreLastQueue() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val ids = prefs.getString("media_ids", null)
            ?.split(',')?.mapNotNull { it.toLongOrNull() }
            .orEmpty()
        if (ids.isEmpty()) return
        val index = prefs.getInt("index", 0).coerceIn(0, ids.size - 1)
        val position = prefs.getLong("position", 0)
        val items = MediaItemBuilder.buildMany(applicationContext, ids)
        if (items.isEmpty()) return
        player?.setMediaItems(items, index, position)
        player?.prepare()
        player?.playWhenReady = false
    }

    private fun sessionActivityIntent(): PendingIntent =
        PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playing when the task is swiped away; Media3 manages the notification.
        val p = player ?: return
        if (!p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
