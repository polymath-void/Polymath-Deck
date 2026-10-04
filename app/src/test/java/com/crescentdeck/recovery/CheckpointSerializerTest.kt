package com.crescentdeck.recovery

import com.crescentdeck.engine.quadtree.CardNode
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckpointSerializerTest {

    @Test
    fun testSerializationRoundTrip() {
        val originalNodes = listOf(
            CardNode("c1", 100f, 200f, 320f, 240f, 100f, 200f, priority = 1, cardType = 0),
            CardNode("c2", 500f, 600f, 320f, 240f, 500f, 600f, priority = 3, cardType = 2)
        )

        val bytes = CheckpointSerializer.serialize(originalNodes)
        val deserialized = CheckpointSerializer.deserialize(bytes)

        assertEquals(2, deserialized.size)
        assertEquals("c1", deserialized[0].id)
        assertEquals(100f, deserialized[0].x, 0.001f)
        assertEquals(200f, deserialized[0].y, 0.001f)
        assertEquals("c2", deserialized[1].id)
        assertEquals(2, deserialized[1].cardType)
    }
}
