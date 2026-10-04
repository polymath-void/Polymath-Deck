package com.crescentdeck.bridge

import com.crescentdeck.engine.physics.SpringDynamicsProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches high-frequency physics ticks from Dispatchers.Default directly into
 * the NodeSelectiveInvalidationState flow channels.
 */
@Singleton
class PhysicsToComposeDispatcher @Inject constructor(
    private val springProcessor: SpringDynamicsProcessor,
    private val invalidationBridge: NodeSelectiveInvalidationState
) {

    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var physicsJob: Job? = null
    private var isRunning: Boolean = false

    /**
     * Starts the physics simulation loop with a target frame interval [tickIntervalMs].
     */
    fun startSimulation(scope: CoroutineScope, tickIntervalMs: Long = 16L) {
        if (isRunning) return
        isRunning = true

        physicsJob = scope.launch(computationDispatcher) {
            val dt = tickIntervalMs.toFloat() / 1000f
            try {
                while (isActive && isRunning) {
                    val startTime = System.nanoTime()

                    // Compute spring forces, collisions, and deltas
                    val deltas = springProcessor.tick(dt)

                    // Emit to isolated card flows
                    if (deltas.isNotEmpty()) {
                        invalidationBridge.batchEmit(deltas)
                    }

                    val elapsedMs = (System.nanoTime() - startTime) / 1_000_000L
                    val sleepMs = (tickIntervalMs - elapsedMs).coerceAtLeast(1L)
                    delay(sleepMs)
                }
            } catch (e: CancellationException) {
                // Cooperative coroutine shutdown
            } finally {
                isRunning = false
            }
        }
    }

    /**
     * Pauses the simulation loop.
     */
    fun stopSimulation() {
        isRunning = false
        physicsJob?.cancel()
        physicsJob = null
    }
}
