package com.spimp3.app.playback

import android.content.Context
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import com.spimp3.app.R

/**
 * Builds the playback notification provider with a branded channel name.
 * Media3's default provider is used (it handles Media3-style controls,
 * lock-screen and Bluetooth integration), with our channel name and id.
 */
fun createNotificationProvider(context: Context): MediaNotification.Provider =
    DefaultMediaNotificationProvider.Builder(context)
        .setChannelId(CHANNEL_ID)
        .setChannelName(R.string.playback_channel_name)
        .setNotificationId(NOTIFICATION_ID)
        .build()

const val CHANNEL_ID = "spimp3_playback"
const val NOTIFICATION_ID = 1001
