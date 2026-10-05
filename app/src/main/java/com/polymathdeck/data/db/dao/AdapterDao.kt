package com.polymathdeck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.polymathdeck.data.db.entity.AdapterEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for card customization adapters and CSS rules.
 */
@Dao
interface AdapterDao {

    @Query("SELECT * FROM adapters WHERE cardId = :cardId LIMIT 1")
    suspend fun getAdapterForCard(cardId: String): AdapterEntity?

    @Query("SELECT * FROM adapters WHERE cardId = :cardId LIMIT 1")
    fun getAdapterForCardFlow(cardId: String): Flow<AdapterEntity?>

    @Upsert
    suspend fun upsertAdapter(adapter: AdapterEntity)

    @Delete
    suspend fun deleteAdapter(adapter: AdapterEntity)

    @Query("DELETE FROM adapters WHERE cardId = :cardId")
    suspend fun deleteAdapterForCard(cardId: String)
}
