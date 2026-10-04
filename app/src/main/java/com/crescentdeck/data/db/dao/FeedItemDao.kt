package com.crescentdeck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.crescentdeck.data.db.entity.FeedItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for individual RSS feed articles.
 */
@Dao
interface FeedItemDao {

    @Query("SELECT * FROM feed_items ORDER BY publishedAt DESC LIMIT :limit")
    fun getRecentItemsFlow(limit: Int = 100): Flow<List<FeedItemEntity>>

    @Query("SELECT * FROM feed_items WHERE feedId = :feedId ORDER BY publishedAt DESC")
    fun getItemsForFeedFlow(feedId: String): Flow<List<FeedItemEntity>>

    @Query("SELECT * FROM feed_items WHERE itemId = :itemId LIMIT 1")
    suspend fun getItemById(itemId: String): FeedItemEntity?

    @Query("SELECT * FROM feed_items WHERE guid = :guid LIMIT 1")
    suspend fun getItemByGuid(guid: String): FeedItemEntity?

    @Query("SELECT guid FROM feed_items WHERE feedId = :feedId")
    suspend fun getExistingGuidsForFeed(feedId: String): List<String>

    @Upsert
    suspend fun upsertItem(item: FeedItemEntity)

    @Upsert
    suspend fun upsertItems(items: List<FeedItemEntity>)

    @Query("UPDATE feed_items SET isRead = :isRead WHERE itemId = :itemId")
    suspend fun markRead(itemId: String, isRead: Boolean = true)

    @Query("UPDATE feed_items SET isStarred = :isStarred WHERE itemId = :itemId")
    suspend fun markStarred(itemId: String, isStarred: Boolean)

    @Query("UPDATE feed_items SET cardId = :cardId WHERE itemId = :itemId")
    suspend fun associateCard(itemId: String, cardId: String)

    @Delete
    suspend fun deleteItem(item: FeedItemEntity)

    @Query("DELETE FROM feed_items WHERE feedId = :feedId")
    suspend fun deleteItemsForFeed(feedId: String)
}
