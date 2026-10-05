package com.polymathdeck.recovery

import com.polymathdeck.data.db.dao.CrashCheckpointDao
import com.polymathdeck.data.db.entity.CrashCheckpointEntity
import com.polymathdeck.engine.quadtree.CardNode
import com.polymathdeck.engine.quadtree.QuadTreePhysicsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Periodically writes heartbeat checkpoints to Room and restores state
 * on cold boot after crash or OS process termination.
 */
@Singleton
class CrashRecoveryManager @Inject constructor(
    private val checkpointDao: CrashCheckpointDao,
    private val quadTreeEngine: QuadTreePhysicsEngine
) {

    private var heartbeatJob: Job? = null

    /**
     * Starts periodic 5-second state checkpointing for [deckId].
     */
    fun startHeartbeat(scope: CoroutineScope, deckId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(5000L)
                val allNodes = quadTreeEngine.getAllNodes()
                if (allNodes.isNotEmpty()) {
                    val bytes = CheckpointSerializer.serialize(allNodes)
                    val checkpoint = CrashCheckpointEntity(
                        deckId = deckId,
                        serializedState = bytes,
                        timestamp = System.currentTimeMillis(),
                        isValid = true
                    )
                    checkpointDao.insertCheckpoint(checkpoint)
                    // Prune checkpoints older than 2 minutes
                    checkpointDao.pruneOldCheckpoints(System.currentTimeMillis() - 120_000L)
                }
            }
        }
    }

    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /**
     * Attempts to restore recent valid state.
     * Returns true if restored, false if clean start should proceed.
     */
    suspend fun attemptRestore(deckId: String, maxAgeMs: Long = 45_000L): Boolean = withContext(Dispatchers.IO) {
        val latest = checkpointDao.getLatestValidCheckpoint(deckId) ?: return@withContext false
        val age = System.currentTimeMillis() - latest.timestamp
        if (age > maxAgeMs) {
            return@withContext false
        }

        try {
            val restoredNodes: List<CardNode> = CheckpointSerializer.deserialize(latest.serializedState)
            if (restoredNodes.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    quadTreeEngine.batchInsert(restoredNodes)
                }
                return@withContext true
            }
        } catch (e: Exception) {
            checkpointDao.invalidateCheckpointsForDeck(deckId)
        }
        return@withContext false
    }
}
