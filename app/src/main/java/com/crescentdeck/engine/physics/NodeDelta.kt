package com.crescentdeck.engine.physics

/**
 * High-frequency per-frame transform delta consumed directly by Modifier.graphicsLayer.
 * Bypasses Compose measure and layout passes.
 */
data class NodeDelta(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationZ: Float = 0f,
    val alpha: Float = 1f
) {
    companion object {
        val ZERO = NodeDelta()
    }
}
