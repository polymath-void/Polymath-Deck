package com.polymathdeck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.polymathdeck.data.db.entity.FeedEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for RSS/Atom feed subscriptions.
 */
@Dao
interface FeedDao {

    @Query("SELECT * FROM feeds ORDER BY title ASC")
    fun getAllFeedsFlow(): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feeds WHERE isActive = 1")
    suspend fun getActiveFeeds(): List<FeedEntity>

    @Query("SELECT * FROM feeds WHERE feedId = :feedId LIMIT 1")
    suspend fun getFeedById(feedId: String): FeedEntity?

    @Query("SELECT * FROM feeds WHERE url = :url LIMIT 1")
    suspend fun getFeedByUrl(url: String): FeedEntity?

    @Upsert
    suspend fun upsertFeed(feed: FeedEntity)

    @Upsert
    suspend fun upsertFeeds(feeds: List<FeedEntity>)

    @Query("UPDATE feeds SET lastRefreshedAt = :timestamp, etag = :etag, lastModified = :lastModified WHERE feedId = :feedId")
    suspend fun updateFeedRefreshMeta(feedId: String, timestamp: Long, etag: String?, lastModified: String?)

    @Delete
    suspend fun deleteFeed(feed: FeedEntity)

    @Query("DELETE FROM feeds WHERE feedId = :feedId")
    suspend fun deleteFeedById(feedId: String)
}
