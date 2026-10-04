package com.crescentdeck.recovery

import com.crescentdeck.engine.quadtree.CardNode
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Serializes and deserializes QuadTree card node positions into compact byte arrays.
 */
object CheckpointSerializer {

    fun serialize(nodes: List<CardNode>): ByteArray {
        val array = JSONArray()
        for (node in nodes) {
            val obj = JSONObject().apply {
                put("id", node.id)
                put("x", node.x.toDouble())
                put("y", node.y.toDouble())
                put("w", node.width.toDouble())
                put("h", node.height.toDouble())
                put("ax", node.anchorX.toDouble())
                put("ay", node.anchorY.toDouble())
                put("p", node.priority)
                put("t", node.cardType)
                put("sleep", node.isSleeping)
            }
            array.put(obj)
        }
        return array.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun deserialize(bytes: ByteArray): List<CardNode> {
        val jsonString = String(bytes, StandardCharsets.UTF_8)
        val array = JSONArray(jsonString)
        val result = mutableListOf<CardNode>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                CardNode(
                    id = obj.getString("id"),
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat(),
                    width = obj.getDouble("w").toFloat(),
                    height = obj.getDouble("h").toFloat(),
                    anchorX = obj.getDouble("ax").toFloat(),
                    anchorY = obj.getDouble("ay").toFloat(),
                    priority = obj.getInt("p"),
                    cardType = obj.getInt("t"),
                    isSleeping = obj.optBoolean("sleep", false)
                )
            )
        }
        return result
    }
}
