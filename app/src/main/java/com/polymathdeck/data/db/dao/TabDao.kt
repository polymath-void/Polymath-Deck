package com.polymathdeck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.polymathdeck.data.db.entity.TabEntity
import com.polymathdeck.data.db.entity.TabGroupEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for tabs and tab groups.
 */
@Dao
interface TabDao {

    @Query("SELECT * FROM tabs ORDER BY sortOrder ASC")
    fun getAllTabsFlow(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE tabId = :tabId LIMIT 1")
    suspend fun getTabById(tabId: String): TabEntity?

    @Query("SELECT * FROM tabs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTab(): TabEntity?

    @Query("SELECT * FROM tabs WHERE isActive = 1 LIMIT 1")
    fun getActiveTabFlow(): Flow<TabEntity?>

    @Query("SELECT * FROM tabs WHERE groupId = :groupId ORDER BY sortOrder ASC")
    fun getTabsByGroupFlow(groupId: String): Flow<List<TabEntity>>

    @Upsert
    suspend fun upsertTab(tab: TabEntity)

    @Upsert
    suspend fun upsertTabs(tabs: List<TabEntity>)

    @Delete
    suspend fun deleteTab(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE tabId = :tabId")
    suspend fun deleteTabById(tabId: String)

    @Query("UPDATE tabs SET isActive = 0")
    suspend fun deactivateAllTabs()

    @Transaction
    suspend fun setActiveTab(tabId: String) {
        deactivateAllTabs()
        activateTabById(tabId, System.currentTimeMillis())
    }

    @Query("UPDATE tabs SET isActive = 1, lastAccessedAt = :timestamp WHERE tabId = :tabId")
    suspend fun activateTabById(tabId: String, timestamp: Long)

    @Query("UPDATE tabs SET isHibernated = :hibernated WHERE tabId = :tabId")
    suspend fun updateTabHibernation(tabId: String, hibernated: Boolean)

    // Tab Groups
    @Query("SELECT * FROM tab_groups ORDER BY sortOrder ASC")
    fun getAllGroupsFlow(): Flow<List<TabGroupEntity>>

    @Upsert
    suspend fun upsertGroup(group: TabGroupEntity)

    @Delete
    suspend fun deleteGroup(group: TabGroupEntity)
}
