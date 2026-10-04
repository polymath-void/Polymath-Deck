package com.crescentdeck.engine.physics

import com.crescentdeck.engine.quadtree.CardNode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * 2D collision detection and separation impulse resolver for spatial card nodes.
 */
object CollisionResolver {

    data class Vector2D(val x: Float, val y: Float)

    /**
     * Calculates the penetration depth and direction between [a] and [b].
     * Returns a separation vector that pushes [a] away from [b].
     */
    fun computePenetrationVector(a: CardNode, b: CardNode): Vector2D {
        val aBox = a.boundingBox
        val bBox = b.boundingBox

        val overlapX = min(aBox.right, bBox.right) - max(aBox.left, bBox.left)
        val overlapY = min(aBox.bottom, bBox.bottom) - max(aBox.top, bBox.top)

        if (overlapX <= 0f || overlapY <= 0f) {
            return Vector2D(0f, 0f)
        }

        // Separate along the axis of least penetration
        return if (overlapX < overlapY) {
            val dirX = if (aBox.centerX < bBox.centerX) -1f else 1f
            Vector2D(dirX * overlapX, 0f)
        } else {
            val dirY = if (aBox.centerY < bBox.centerY) -1f else 1f
            Vector2D(0f, dirY * overlapY)
        }
    }

    /**
     * Calculates the impulse to apply to node [a] to separate it from neighbor [b],
     * scaled by priority weighting and separation coefficient.
     */
    fun resolveSeparationImpulse(
        a: CardNode,
        b: CardNode,
        separationStrength: Float = 800f
    ): Vector2D {
        val pen = computePenetrationVector(a, b)
        if (pen.x == 0f && pen.y == 0f) return Vector2D(0f, 0f)

        // Lower priority nodes yield more easily to higher priority nodes
        val weightA = max(1, a.priority).toFloat()
        val weightB = max(1, b.priority).toFloat()
        val ratio = weightB / (weightA + weightB)

        return Vector2D(
            x = pen.x * ratio * (separationStrength / 100f),
            y = pen.y * ratio * (separationStrength / 100f)
        )
    }
}
