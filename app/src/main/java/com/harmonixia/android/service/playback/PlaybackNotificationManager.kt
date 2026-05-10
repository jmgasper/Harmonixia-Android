package com.harmonixia.android.service.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.harmonixia.android.R
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification

@UnstableApi
class PlaybackNotificationManager(
    private val context: Context
) {
    val notificationProvider: MediaNotification.Provider =
        DefaultMediaNotificationProvider.Builder(context)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setNotificationId(NOTIFICATION_ID)
            .build()

    fun ensureNotificationChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.playback_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "harmonixia_playback"
        private const val NOTIFICATION_ID = 1001
    }
}
