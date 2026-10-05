package com.polymathdeck.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.polymathdeck.data.db.entity.CrashCheckpointEntity

/**
 * Data Access Object for serialized crash checkpoints.
 */
@Dao
interface CrashCheckpointDao {

    @Query("SELECT * FROM crash_checkpoints WHERE deckId = :deckId AND isValid = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestValidCheckpoint(deckId: String): CrashCheckpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoint(checkpoint: CrashCheckpointEntity): Long

    @Query("UPDATE crash_checkpoints SET isValid = 0 WHERE deckId = :deckId")
    suspend fun invalidateCheckpointsForDeck(deckId: String)

    @Query("DELETE FROM crash_checkpoints WHERE timestamp < :cutoffTime")
    suspend fun pruneOldCheckpoints(cutoffTime: Long)

    @Query("DELETE FROM crash_checkpoints WHERE deckId = :deckId")
    suspend fun deleteCheckpointsForDeck(deckId: String)
}
