package com.polymathdeck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.polymathdeck.data.db.entity.MediaSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for persistent media playback sessions.
 */
@Dao
interface MediaSessionDao {

    @Query("SELECT * FROM media_sessions WHERE cardId = :cardId LIMIT 1")
    suspend fun getSessionByCardId(cardId: String): MediaSessionEntity?

    @Query("SELECT * FROM media_sessions WHERE cardId = :cardId LIMIT 1")
    fun getSessionByCardIdFlow(cardId: String): Flow<MediaSessionEntity?>

    @Query("SELECT * FROM media_sessions WHERE isPlaying = 1 LIMIT 1")
    suspend fun getCurrentlyPlayingSession(): MediaSessionEntity?

    @Query("SELECT * FROM media_sessions WHERE isPlaying = 1 LIMIT 1")
    fun getCurrentlyPlayingSessionFlow(): Flow<MediaSessionEntity?>

    @Upsert
    suspend fun upsertSession(session: MediaSessionEntity)

    @Query("UPDATE media_sessions SET positionMs = :pos, updatedAt = :timestamp WHERE cardId = :cardId")
    suspend fun updatePosition(cardId: String, pos: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE media_sessions SET isPlaying = :playing WHERE cardId = :cardId")
    suspend fun updatePlayingState(cardId: String, playing: Boolean)

    @Delete
    suspend fun deleteSession(session: MediaSessionEntity)

    @Query("DELETE FROM media_sessions WHERE cardId = :cardId")
    suspend fun deleteSessionByCardId(cardId: String)
}
