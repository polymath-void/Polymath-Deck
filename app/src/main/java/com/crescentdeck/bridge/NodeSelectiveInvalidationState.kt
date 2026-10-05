package com.crescentdeck.bridge

import com.crescentdeck.engine.physics.NodeDelta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance state bridge providing isolated MutableStateFlow channels per card.
 *
 * This is the critical architectural boundary:
 * Instead of triggering Recomposition on the parent DeckCanvasScreen, physics transformations
 * mutate individual NodeDelta flows. Compose's Modifier.graphicsLayer reads these values directly
 * on the render thread, bypassing measure and layout passes completely.
 */
@Singleton
class NodeSelectiveInvalidationState @Inject constructor() {

    private val deltaFlows = ConcurrentHashMap<String, MutableStateFlow<NodeDelta>>()

    /**
     * Retrieves or creates an isolated StateFlow<NodeDelta> for [cardId].
     */
    fun getDeltaFlow(cardId: String): StateFlow<NodeDelta> {
        return deltaFlows.computeIfAbsent(cardId) {
            MutableStateFlow(NodeDelta.ZERO)
        }.asStateFlow()
    }

    /**
     * Emits a transform update to a specific card's flow.
     */
    fun emitDelta(cardId: String, delta: NodeDelta) {
        val flow = deltaFlows[cardId]
        if (flow != null) {
            flow.value = delta
        } else {
            deltaFlows[cardId] = MutableStateFlow(delta)
        }
    }

    /**
     * Batch dispatches physics calculation results from a single tick.
     */
    fun batchEmit(deltas: Map<String, NodeDelta>) {
        for ((cardId, delta) in deltas) {
            val flow = deltaFlows[cardId]
            if (flow != null) {
                flow.value = delta
            } else {
                deltaFlows[cardId] = MutableStateFlow(delta)
            }
        }
    }

    private val _floatingDeltas = MutableStateFlow<Map<String, NodeDelta>>(emptyMap())
    val floatingDeltas: StateFlow<Map<String, NodeDelta>> = _floatingDeltas.asStateFlow()

    /**
     * Emits a transform update to a detached floating card (e.g. PiP overlay).
     */
    fun emitFloatingDelta(cardId: String, delta: NodeDelta) {
        val current = _floatingDeltas.value.toMutableMap()
        current[cardId] = delta
        _floatingDeltas.value = current
    }

    /**
     * Removes a card from floating state when returning to grid flow.
     */
    fun removeFloatingCard(cardId: String) {
        val current = _floatingDeltas.value.toMutableMap()
        current.remove(cardId)
        _floatingDeltas.value = current
    }

    /**
     * Cleans up flows for removed cards to prevent memory retention.
     */
    fun removeCard(cardId: String) {
        deltaFlows.remove(cardId)
        removeFloatingCard(cardId)
    }

    fun clear() {
        deltaFlows.clear()
        _floatingDeltas.value = emptyMap()
    }
}
