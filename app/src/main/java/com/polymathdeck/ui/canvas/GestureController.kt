package com.polymathdeck.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.polymathdeck.engine.quadtree.CardNode

/**
 * Controller mapping screen pointer gestures into 2D world spatial updates.
 */
class GestureController(
    var viewportOffsetX: Float = 0f,
    var viewportOffsetY: Float = 0f,
    var zoomScale: Float = 1.0f
) {

    /**
     * Converts a screen coordinate [screenOffset] to the canvas 2D world coordinate.
     */
    fun screenToWorld(screenOffset: Offset): Offset {
        val worldX = (screenOffset.x / zoomScale) - viewportOffsetX
        val worldY = (screenOffset.y / zoomScale) - viewportOffsetY
        return Offset(worldX, worldY)
    }

    /**
     * Finds which card node was tapped or dragged at [screenOffset].
     */
    fun hitTest(screenOffset: Offset, nodes: List<CardNode>): CardNode? {
        val world = screenToWorld(screenOffset)
        // Check in reverse order so higher-drawn cards receive the hit first
        for (i in nodes.indices.reversed()) {
            val node = nodes[i]
            if (node.boundingBox.containsPoint(world.x, world.y)) {
                return node
            }
        }
        return null
    }

    fun onPan(delta: Offset) {
        viewportOffsetX += delta.x / zoomScale
        viewportOffsetY += delta.y / zoomScale
    }

    fun onZoom(zoomFactor: Float) {
        zoomScale = (zoomScale * zoomFactor).coerceIn(0.25f, 3.0f)
    }
}
