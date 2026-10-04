package com.crescentdeck.data.repository

import com.crescentdeck.data.db.dao.MediaSessionDao
import com.crescentdeck.data.db.entity.MediaSessionEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for media playback session persistence and state synchronization.
 */
@Singleton
class MediaRepository @Inject constructor(
    private val mediaSessionDao: MediaSessionDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getSessionByCardIdFlow(cardId: String): Flow<MediaSessionEntity?> =
        mediaSessionDao.getSessionByCardIdFlow(cardId)

    suspend fun getSessionByCardId(cardId: String): MediaSessionEntity? = withContext(ioDispatcher) {
        mediaSessionDao.getSessionByCardId(cardId)
    }

    fun getCurrentlyPlayingSessionFlow(): Flow<MediaSessionEntity?> =
        mediaSessionDao.getCurrentlyPlayingSessionFlow()

    suspend fun getCurrentlyPlayingSession(): MediaSessionEntity? = withContext(ioDispatcher) {
        mediaSessionDao.getCurrentlyPlayingSession()
    }

    suspend fun upsertSession(session: MediaSessionEntity) = withContext(ioDispatcher) {
        mediaSessionDao.upsertSession(session)
    }

    suspend fun updatePosition(cardId: String, pos: Long) = withContext(ioDispatcher) {
        mediaSessionDao.updatePosition(cardId, pos)
    }

    suspend fun updatePlayingState(cardId: String, isPlaying: Boolean) = withContext(ioDispatcher) {
        mediaSessionDao.updatePlayingState(cardId, isPlaying)
    }

    suspend fun deleteSessionByCardId(cardId: String) = withContext(ioDispatcher) {
        mediaSessionDao.deleteSessionByCardId(cardId)
    }
}
