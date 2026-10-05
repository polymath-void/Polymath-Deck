package com.crescentdeck.engine

import com.crescentdeck.engine.quadtree.BoundingBox
import com.crescentdeck.engine.quadtree.CardNode
import com.crescentdeck.engine.quadtree.QuadTreePhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuadTreePhysicsEngineTest {

    private lateinit var engine: QuadTreePhysicsEngine

    @Before
    fun setUp() {
        engine = QuadTreePhysicsEngine()
        engine.setBounds(BoundingBox(-1000f, -1000f, 2000f, 2000f))
    }

    @Test
    fun testInsertionAndQuery() {
        val node1 = CardNode(
            id = "c1",
            x = 50f, y = 50f,
            width = 100f, height = 100f,
            anchorX = 50f, anchorY = 50f
        )
        val node2 = CardNode(
            id = "c2",
            x = 800f, y = 800f,
            width = 100f, height = 100f,
            anchorX = 800f, anchorY = 800f
        )

        engine.insertNode(node1)
        engine.insertNode(node2)

        val queryArea = BoundingBox(0f, 0f, 200f, 200f)
        val results = engine.queryRange(queryArea)

        assertEquals(1, results.size)
        assertEquals("c1", results[0].id)
    }

    @Test
    fun testLocateNode() {
        val node = CardNode(
            id = "card_test",
            x = 10f, y = 20f,
            width = 50f, height = 50f,
            anchorX = 10f, anchorY = 20f
        )
        engine.insertNode(node)

        val located = engine.locateNode("card_test")
        assertNotNull(located)
        assertEquals(10f, located!!.x, 0.001f)
    }

    @Test
    fun testBatchInsertAndNeighborQuery() {
        val nodes = (0 until 10).map { i ->
            CardNode(
                id = "n_$i",
                x = (i * 10).toFloat(),
                y = 0f,
                width = 20f,
                height = 20f,
                anchorX = (i * 10).toFloat(),
                anchorY = 0f
            )
        }
        engine.batchInsert(nodes)

        val neighbors = engine.queryNeighbors(nodes[0], margin = 15f)
        assertTrue(neighbors.isNotEmpty())
        assertTrue(neighbors.any { it.id == "n_1" })
    }

    @Test
    fun testDetachedNodeExcludedFromSpatialQuery() {
        val node1 = CardNode(id = "attached", x = 10f, y = 10f, width = 50f, height = 50f, anchorX = 10f, anchorY = 10f, isDetached = false)
        val node2 = CardNode(id = "detached", x = 10f, y = 10f, width = 50f, height = 50f, anchorX = 10f, anchorY = 10f, isDetached = true)

        engine.insertNode(node1)
        engine.insertNode(node2)

        val results = engine.queryRange(BoundingBox(0f, 0f, 100f, 100f))
        assertEquals(1, results.size)
        assertEquals("attached", results[0].id)
    }
}
