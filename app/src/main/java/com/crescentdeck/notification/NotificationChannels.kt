package com.crescentdeck.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.crescentdeck.R

/**
 * Initializes and registers all notification channels on Android O+.
 */
object NotificationChannels {

    const val CHANNEL_MEDIA = "crescent_media"
    const val CHANNEL_RSS = "crescent_rss"
    const val CHANNEL_SYSTEM = "crescent_system"
    const val CHANNEL_DOWNLOAD = "crescent_download"

    fun registerAll(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mediaChannel = NotificationChannel(
                CHANNEL_MEDIA,
                context.getString(R.string.channel_media_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_media_desc)
                setShowBadge(false)
            }

            val rssChannel = NotificationChannel(
                CHANNEL_RSS,
                context.getString(R.string.channel_rss_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_rss_desc)
                setShowBadge(true)
            }

            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                context.getString(R.string.channel_system_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_system_desc)
            }

            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                context.getString(R.string.channel_download_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_download_desc)
            }

            notificationManager.createNotificationChannels(
                listOf(mediaChannel, rssChannel, systemChannel, downloadChannel)
            )
        }
    }
}
