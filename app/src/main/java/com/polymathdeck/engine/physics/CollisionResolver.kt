package com.polymathdeck.engine.physics

import com.polymathdeck.engine.quadtree.CardNode
import kotlin.math.max
import kotlin.math.min

/**
 * 2D collision detection and separation impulse resolver for spatial card nodes.
 * Enforces dynamic CollisionGutter: 16dp static gutter vs 24dp active drag magnetic forcefield.
 */
object CollisionResolver {

    data class Vector2D(val x: Float, val y: Float) {
        companion object {
            val ZERO = Vector2D(0f, 0f)
        }
    }

    const val STATIC_GUTTER: Float = 16f
    const val ACTIVE_DRAG_GUTTER: Float = 24f

    /**
     * Computes the effective gutter between nodes [a] and [b].
     * When either card is being actively dragged, the gutter increases to 24dp,
     * creating a magnetic forcefield and clear drop zone.
     */
    fun getEffectiveGutter(a: CardNode, b: CardNode): Float {
        return if (a.isBeingDragged || b.isBeingDragged) ACTIVE_DRAG_GUTTER else STATIC_GUTTER
    }

    /**
     * Calculates the penetration depth and direction between [a] and [b]
     * with the specified [gutter].
     * Returns a separation vector that pushes [a] away from [b].
     */
    fun computePenetrationVector(a: CardNode, b: CardNode, gutter: Float = getEffectiveGutter(a, b)): Vector2D {
        val aBox = a.boundingBox
        val bBox = b.boundingBox

        val overlapX = min(aBox.right, bBox.right) - max(aBox.left, bBox.left) + gutter
        val overlapY = min(aBox.bottom, bBox.bottom) - max(aBox.top, bBox.top) + gutter

        if (overlapX <= 0f || overlapY <= 0f) {
            return Vector2D.ZERO
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
     * scaled by priority weighting, active drag state, and separation coefficient.
     */
    fun resolveSeparationImpulse(
        a: CardNode,
        b: CardNode,
        separationStrength: Float = 800f,
        gutter: Float = getEffectiveGutter(a, b)
    ): Vector2D {
        if (a.isDetached || b.isDetached) return Vector2D.ZERO

        val pen = computePenetrationVector(a, b, gutter)
        if (pen.x == 0f && pen.y == 0f) return Vector2D.ZERO

        // When [a] is held by the user (being dragged), [a] doesn't yield to impulse;
        // neighbor [b] yields 100%.
        if (a.isBeingDragged) {
            return Vector2D.ZERO
        }

        val ratio = if (b.isBeingDragged) {
            // b is being dragged into a: a yields 100% to create the magnetic drop zone
            1.0f
        } else {
            // Lower priority nodes yield more easily to higher priority nodes
            val weightA = max(1, a.priority).toFloat()
            val weightB = max(1, b.priority).toFloat()
            weightB / (weightA + weightB)
        }

        return Vector2D(
            x = pen.x * ratio * (separationStrength / 100f),
            y = pen.y * ratio * (separationStrength / 100f)
        )
    }
}
