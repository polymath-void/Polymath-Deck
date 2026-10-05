package com.polymathdeck.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.polymathdeck.R
import com.polymathdeck.data.db.entity.FeedItemEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches system notifications for RSS updates, download progress, and crash recovery.
 */
@Singleton
class NotificationDispatcher @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showRssNotification(feedTitle: String, item: FeedItemEntity) {
        val cardId = item.cardId ?: item.itemId
        val openIntent = DeepLinkRouter.createIntentForCard(context, cardId)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            item.itemId.hashCode(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_RSS)
            .setSmallIcon(R.drawable.ic_rss)
            .setContentTitle(item.title)
            .setContentText(feedTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.extractedText.take(200)))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_read, "Read", contentPendingIntent)
            .setGroup("GROUP_POLYMATH_RSS")
            .build()

        notificationManager.notify(item.itemId.hashCode(), notification)
    }

    fun showCrashRecoveryNotification(deckId: String) {
        val openIntent = Intent(context, com.polymathdeck.ui.screen.MainActivity::class.java).apply {
            data = DeepLinkRouter.createDeckDeepLink(deckId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_SYSTEM)
            .setSmallIcon(R.drawable.ic_polymath)
            .setContentTitle("Session Restored")
            .setContentText("Polymath Deck restored your workspace following a sudden shutdown.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(999, notification)
    }
}
