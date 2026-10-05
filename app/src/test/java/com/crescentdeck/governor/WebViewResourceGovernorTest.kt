package com.crescentdeck.governor

import com.crescentdeck.engine.governor.CardLifecycleState
import com.crescentdeck.engine.governor.CardResourceState
import com.crescentdeck.engine.governor.MemoryPressureMonitor
import com.crescentdeck.engine.governor.WebViewResourceGovernor
import com.crescentdeck.engine.quadtree.CardNode
import com.crescentdeck.engine.quadtree.QuadTreePhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebViewResourceGovernorTest {

    private lateinit var governor: WebViewResourceGovernor
    private lateinit var quadTree: QuadTreePhysicsEngine

    @Before
    fun setUp() {
        val monitor = MemoryPressureMonitor()
        governor = WebViewResourceGovernor(monitor)
        quadTree = QuadTreePhysicsEngine()
        governor.quadTreeEngine = quadTree
    }

    @Test
    fun testLRUWatchdogLimitsActiveCardsToSix() {
        // Register 7 cards in sequential order
        for (i in 1..7) {
            val cardId = "card_$i"
            val node = CardNode(
                id = cardId,
                x = (i * 50).toFloat(),
                y = 0f,
                width = 50f,
                height = 50f,
                anchorX = (i * 50).toFloat(),
                anchorY = 0f
            )
            quadTree.insertNode(node)
            governor.transitionTo(cardId, CardLifecycleState.GRID_FLOW)
            Thread.sleep(5) // Ensure distinct timestamps
        }

        // Card 1 was the oldest, so it must have been evicted to MINIMIZED
        val lifecycle = governor.lifecycleEvents.value
        assertEquals(CardLifecycleState.MINIMIZED, lifecycle["card_1"])

        // Cards 2 through 7 should remain active
        for (i in 2..7) {
            assertEquals(CardLifecycleState.GRID_FLOW, lifecycle["card_$i"])
        }

        // Active cards count should be exactly maxActiveWebViews (6)
        val activeCount = governor.governorEvents.value.count { it.value == CardResourceState.ACTIVE }
        assertEquals(6, activeCount)
    }

    @Test
    fun testImmersiveCardsImmuneToLRUEviction() {
        // Set card_1 to IMMERSIVE
        val node1 = CardNode(id = "card_1", x = 0f, y = 0f, width = 50f, height = 50f, anchorX = 0f, anchorY = 0f)
        quadTree.insertNode(node1)
        governor.transitionTo("card_1", CardLifecycleState.IMMERSIVE)

        // Now add 6 more cards
        for (i in 2..7) {
            val cardId = "card_$i"
            val node = CardNode(id = cardId, x = (i * 50).toFloat(), y = 0f, width = 50f, height = 50f, anchorX = (i * 50).toFloat(), anchorY = 0f)
            quadTree.insertNode(node)
            governor.transitionTo(cardId, CardLifecycleState.GRID_FLOW)
            Thread.sleep(5)
        }

        // Card 1 must remain IMMERSIVE and not be evicted!
        val lifecycle = governor.lifecycleEvents.value
        assertEquals(CardLifecycleState.IMMERSIVE, lifecycle["card_1"])
    }

    @Test
    fun testPiPDetachmentSetsFloatingAndDetached() {
        val node = CardNode(id = "pip_card", x = 10f, y = 10f, width = 100f, height = 100f, anchorX = 10f, anchorY = 10f)
        quadTree.insertNode(node)
        governor.transitionTo("pip_card", CardLifecycleState.GRID_FLOW)
        assertFalse(node.isDetached)

        governor.detachToPiP("pip_card")
        assertTrue(governor.floatingCards.value.contains("pip_card"))
        assertTrue(node.isDetached)

        governor.attachFromPiP("pip_card")
        assertFalse(governor.floatingCards.value.contains("pip_card"))
        assertFalse(node.isDetached)
    }
}
