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

    @Test
    fun testDynamicCollisionGutterExpandsDropZone() {
        // Distance between right edge of a (50) and left edge of b (60) is 10 units.
        val a = CardNode(id = "a", x = 0f, y = 0f, width = 50f, height = 50f, anchorX = 0f, anchorY = 0f)
        val bStatic = CardNode(id = "b", x = 60f, y = 0f, width = 50f, height = 50f, anchorX = 60f, anchorY = 0f, isBeingDragged = false)
        val bDragged = CardNode(id = "b_drag", x = 60f, y = 0f, width = 50f, height = 50f, anchorX = 60f, anchorY = 0f, isBeingDragged = true)

        val staticImpulse = CollisionResolver.resolveSeparationImpulse(a, bStatic)
        val dragImpulse = CollisionResolver.resolveSeparationImpulse(a, bDragged)

        // Active drag gutter (24dp) causes stronger repulsion than static gutter (16dp)
        assertTrue(dragImpulse.x < staticImpulse.x)
    }

    @Test
    fun testDetachedNodeReturnsZeroImpulse() {
        val a = CardNode(id = "a", x = 0f, y = 0f, width = 50f, height = 50f, anchorX = 0f, anchorY = 0f, isDetached = true)
        val b = CardNode(id = "b", x = 20f, y = 0f, width = 50f, height = 50f, anchorX = 20f, anchorY = 0f)

        val impulse = CollisionResolver.resolveSeparationImpulse(a, b)
        assertEquals(0f, impulse.x, 0.001f)
        assertEquals(0f, impulse.y, 0.001f)
    }
}
