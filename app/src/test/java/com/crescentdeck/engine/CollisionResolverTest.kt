package com.crescentdeck.engine

import com.crescentdeck.engine.physics.CollisionResolver
import com.crescentdeck.engine.quadtree.CardNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionResolverTest {

    @Test
    fun testNonOverlappingNodesReturnZeroImpulse() {
        val a = CardNode(id = "a", x = 0f, y = 0f, width = 50f, height = 50f, anchorX = 0f, anchorY = 0f)
        val b = CardNode(id = "b", x = 100f, y = 100f, width = 50f, height = 50f, anchorX = 100f, anchorY = 100f)

        val impulse = CollisionResolver.resolveSeparationImpulse(a, b)
        assertEquals(0f, impulse.x, 0.001f)
        assertEquals(0f, impulse.y, 0.001f)
    }

    @Test
    fun testOverlappingNodesGenerateSeparationVector() {
        // Overlapping along X axis
        val a = CardNode(id = "a", x = 0f, y = 0f, width = 50f, height = 50f, anchorX = 0f, anchorY = 0f, priority = 1)
        val b = CardNode(id = "b", x = 30f, y = 0f, width = 50f, height = 50f, anchorX = 30f, anchorY = 0f, priority = 1)

        val impulse = CollisionResolver.resolveSeparationImpulse(a, b)
        // a should be pushed left (negative X)
        assertTrue(impulse.x < 0f)
    }
}
