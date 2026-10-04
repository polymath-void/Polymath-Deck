package com.crescentdeck.engine.media

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class PlayerSlot {
    PRIMARY,
    SECONDARY,
    PIP
}

/**
 * Memory-conscious pool of ExoPlayer instances capped at 3 concurrent players.
 */
@UnstableApi
@Singleton
class ExoPlayerPool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adaptiveTrackSelector: AdaptiveTrackSelector
) {

    private val playerPool = ConcurrentHashMap<PlayerSlot, ExoPlayer>()
    private val slotAssignments = ConcurrentHashMap<String, PlayerSlot>()

    /**
     * Acquires or allocates an ExoPlayer instance for the specified [slot].
     */
    @Synchronized
    fun acquirePlayer(cardId: String, slot: PlayerSlot): ExoPlayer {
        // Release any existing assignment for this slot
        playerPool[slot]?.let { existing ->
            if (existing.isPlaying) {
                existing.stop()
            }
            existing.release()
            playerPool.remove(slot)
        }

        val trackSelector = adaptiveTrackSelector.createTrackSelector(context)

        // Memory-tuned LoadControl: 15s min buffer, 30s max buffer
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 30_000, 2_500, 5_000)
            .build()

        val newPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()

        playerPool[slot] = newPlayer
        slotAssignments[cardId] = slot
        return newPlayer
    }

    fun getPlayer(slot: PlayerSlot): ExoPlayer? = playerPool[slot]

    fun getPlayerForCard(cardId: String): ExoPlayer? {
        val slot = slotAssignments[cardId] ?: return null
        return playerPool[slot]
    }

    @Synchronized
    fun releaseCard(cardId: String) {
        val slot = slotAssignments.remove(cardId) ?: return
        playerPool.remove(slot)?.let { player ->
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }

    @Synchronized
    fun releaseAll() {
        slotAssignments.clear()
        for (player in playerPool.values) {
            player.stop()
            player.clearMediaItems()
            player.release()
        }
        playerPool.clear()
    }
}
