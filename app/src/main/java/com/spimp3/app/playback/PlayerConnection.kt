package com.spimp3.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI-side handle to the playback session. One instance per app process.
 * Exposes everything the UI needs as StateFlows and forwards commands.
 */
class PlayerConnection(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    var controller: MediaController? = null
        private set

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSongId = MutableStateFlow<Long?>(null)
    val currentSongId: StateFlow<Long?> = _currentSongId.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentSongId.value = mediaItem?.mediaId?.toLongOrNull()
            _durationMs.value = controller?.duration?.takeIf { it > 0 } ?: 0L
            _currentIndex.value = controller?.currentMediaItemIndex ?: 0
            refreshQueue()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _durationMs.value = controller?.duration?.takeIf { it > 0 } ?: 0L
        }

        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
            _shuffle.value = enabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            refreshQueue()
        }
    }

    suspend fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controller = MediaController.Builder(context, token).buildAsync().await()
        controller?.addListener(playerListener)
        _connected.value = true
        _currentSongId.value = controller?.currentMediaItem?.mediaId?.toLongOrNull()
        _shuffle.value = controller?.shuffleModeEnabled ?: false
        _repeatMode.value = controller?.repeatMode ?: Player.REPEAT_MODE_OFF
        refreshQueue()
        // Position ticker (250ms) while connected
        scope.launch {
            while (isActive) {
                controller?.let {
                    _positionMs.value = it.currentPosition.coerceAtLeast(0)
                    if (it.duration > 0) _durationMs.value = it.duration
                }
                delay(250)
            }
        }
    }

    fun refreshQueue() {
        val c = controller ?: return
        _queue.value = (0 until c.mediaItemCount).map { c.getMediaItemAt(it) }
        _currentIndex.value = c.currentMediaItemIndex
    }

    fun playQueue(songIds: List<Long>, startIndex: Int = 0) {
        val c = controller ?: return
        if (songIds.isEmpty()) return
        scope.launch {
            val items = MediaItemBuilder.buildMany(context, songIds)
            if (items.isEmpty()) return@launch
            c.setMediaItems(items, startIndex.coerceIn(0, items.size - 1), 0L)
            c.prepare()
            c.play()
        }
    }

    fun playSong(songId: Long) = playQueue(listOf(songId), 0)

    fun addToQueue(songIds: List<Long>) {
        val c = controller ?: return
        scope.launch {
            c.addMediaItems(MediaItemBuilder.buildMany(context, songIds))
        }
    }

    fun playNext(songIds: List<Long>) {
        val c = controller ?: return
        scope.launch {
            val items = MediaItemBuilder.buildMany(context, songIds)
            if (items.isEmpty()) return@launch
            c.addMediaItems((c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount), items)
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() = controller?.seekToNextMediaItem()

    fun previous() {
        val c = controller ?: return
        // Standard behaviour: restart current track if >3s in, else go to previous
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceIn(0, durationMs.value.coerceAtLeast(0)))
    }

    fun seekToIndex(index: Int) = controller?.seekTo(index, 0)

    fun removeFromQueue(index: Int) = controller?.removeMediaItem(index)

    fun moveQueueItem(from: Int, to: Int) = controller?.moveMediaItem(from, to)

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _speed.value = speed
    }

    fun setSleepTimer(minutes: Int) {
        controller?.sendCustomCommand(
            SessionCommand(PlaybackService.CUSTOM_COMMAND_SLEEP_TIMER, android.os.Bundle.EMPTY),
            android.os.Bundle().apply { putInt("minutes", minutes) },
        )
    }

    fun release() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        _connected.value = false
    }
}
