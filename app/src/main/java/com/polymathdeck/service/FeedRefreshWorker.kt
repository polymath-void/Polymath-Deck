package com.polymathdeck.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.polymathdeck.bridge.FeedRefreshEvent
import com.polymathdeck.bridge.FeedUpdateState
import com.polymathdeck.engine.feed.RSSFeedEngine
import com.polymathdeck.notification.NotificationDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background WorkManager worker that periodically refreshes active RSS feeds
 * using HTTP conditional GET requests.
 */
@HiltWorker
class FeedRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val feedEngine: RSSFeedEngine,
    private val feedUpdateState: FeedUpdateState,
    private val notifDispatcher: NotificationDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        feedUpdateState.setRefreshing(true)
        val activeFeeds = feedEngine.getActiveFeeds()
        var totalNewArticles = 0

        // Strict sequential iteration to prevent network/memory spikes on mobile
        for (feed in activeFeeds) {
            try {
                val newItems = feedEngine.refreshFeed(feed)
                if (newItems.isNotEmpty()) {
                    totalNewArticles += newItems.size
                    feedUpdateState.emitRefreshEvent(
                        FeedRefreshEvent(feed.feedId, newItems.size)
                    )
                    // Notify user with deep link action
                    notifDispatcher.showRssNotification(feed.title, newItems.first())
                }
            } catch (e: Exception) {
                // Log and gracefully continue next feed
            }
        }

        feedUpdateState.setRefreshing(false)
        return Result.success()
    }
}
