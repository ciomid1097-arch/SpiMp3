package com.spimp3.app.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * External entry point for sleep-timer control (e.g. from tile/widgets/automation apps).
 * forwards the command to [PlaybackService] via onStartCommand.
 */
class SsmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, PlaybackService::class.java)
            .setAction(PlaybackService.ACTION_SSM)
            .putExtra(PlaybackService.EXTRA_MINUTES, intent.getIntExtra(PlaybackService.EXTRA_MINUTES, 0))
        context.startForegroundService(service)
    }
}
