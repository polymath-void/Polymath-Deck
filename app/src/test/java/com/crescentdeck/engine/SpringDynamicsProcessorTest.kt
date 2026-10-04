package com.crescentdeck.engine

import com.crescentdeck.engine.physics.SpringDynamicsProcessor
import com.crescentdeck.engine.quadtree.CardNode
import com.crescentdeck.engine.quadtree.QuadTreePhysicsEngine
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpringDynamicsProcessorTest {

    private lateinit var quadTree: QuadTreePhysicsEngine
    private lateinit var processor: SpringDynamicsProcessor

    @Before
    fun setUp() {
        quadTree = QuadTreePhysicsEngine()
        processor = SpringDynamicsProcessor(quadTree)
    }

    @Test
    fun testSpringReturnsDisplacedNodeTowardAnchor() {
        // Node displaced to the right: x = 100, anchor = 0
        val node = CardNode(
            id = "displaced",
            x = 100f, y = 0f,
            width = 50f, height = 50f,
            anchorX = 0f, anchorY = 0f
        )
        quadTree.insertNode(node)

        // Run one tick
        val deltas = processor.tick(dt = 0.016f)

        assertNotNull(deltas["displaced"])
        // Velocity should now be directed toward anchor (negative vx)
        assertTrue(node.vx < 0f)
    }
}
