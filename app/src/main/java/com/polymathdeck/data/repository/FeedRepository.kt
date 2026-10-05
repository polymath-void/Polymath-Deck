package com.polymathdeck.data.repository

import com.polymathdeck.data.db.dao.FeedDao
import com.polymathdeck.data.db.dao.FeedItemDao
import com.polymathdeck.data.db.entity.FeedEntity
import com.polymathdeck.data.db.entity.FeedItemEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing RSS/Atom feed subscriptions and fetched articles.
 */
@Singleton
class FeedRepository @Inject constructor(
    private val feedDao: FeedDao,
    private val feedItemDao: FeedItemDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getAllFeedsFlow(): Flow<List<FeedEntity>> = feedDao.getAllFeedsFlow()

    suspend fun getActiveFeeds(): List<FeedEntity> = withContext(ioDispatcher) {
        feedDao.getActiveFeeds()
    }

    suspend fun getFeedById(feedId: String): FeedEntity? = withContext(ioDispatcher) {
        feedDao.getFeedById(feedId)
    }

    suspend fun getFeedByUrl(url: String): FeedEntity? = withContext(ioDispatcher) {
        feedDao.getFeedByUrl(url)
    }

    suspend fun upsertFeed(feed: FeedEntity) = withContext(ioDispatcher) {
        feedDao.upsertFeed(feed)
    }

    suspend fun updateFeedRefreshMeta(
        feedId: String,
        timestamp: Long,
        etag: String?,
        lastModified: String?
    ) = withContext(ioDispatcher) {
        feedDao.updateFeedRefreshMeta(feedId, timestamp, etag, lastModified)
    }

    suspend fun deleteFeed(feedId: String) = withContext(ioDispatcher) {
        feedItemDao.deleteItemsForFeed(feedId)
        feedDao.deleteFeedById(feedId)
    }

    fun getRecentItemsFlow(limit: Int = 100): Flow<List<FeedItemEntity>> =
        feedItemDao.getRecentItemsFlow(limit)

    fun getItemsForFeedFlow(feedId: String): Flow<List<FeedItemEntity>> =
        feedItemDao.getItemsForFeedFlow(feedId)

    suspend fun getExistingGuidsForFeed(feedId: String): List<String> = withContext(ioDispatcher) {
        feedItemDao.getExistingGuidsForFeed(feedId)
    }

    suspend fun upsertItems(items: List<FeedItemEntity>) = withContext(ioDispatcher) {
        feedItemDao.upsertItems(items)
    }

    suspend fun markItemRead(itemId: String, isRead: Boolean = true) = withContext(ioDispatcher) {
        feedItemDao.markRead(itemId, isRead)
    }

    suspend fun markItemStarred(itemId: String, isStarred: Boolean) = withContext(ioDispatcher) {
        feedItemDao.markStarred(itemId, isStarred)
    }

    suspend fun associateCard(itemId: String, cardId: String) = withContext(ioDispatcher) {
        feedItemDao.associateCard(itemId, cardId)
    }
}
