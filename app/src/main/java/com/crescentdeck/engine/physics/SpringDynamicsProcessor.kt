package com.crescentdeck.engine.physics

import com.crescentdeck.engine.quadtree.CardNode
import com.crescentdeck.engine.quadtree.QuadTreePhysicsEngine
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance damped harmonic oscillator physics processor.
 * Computes spring return forces, damping, and collision repulsion per tick.
 */
@Singleton
class SpringDynamicsProcessor @Inject constructor(
    private val quadTreeEngine: QuadTreePhysicsEngine
) {

    var springStiffness: Float = 120.0f
    var dampingCoefficient: Float = 0.82f
    var separationStrength: Float = 800.0f
    var sleepThresholdVelocity: Float = 0.5f
    var positionEpsilon: Float = 0.1f
    var collisionMargin: Float = 8.0f

    /**
     * Executes a single physics simulation tick for all active nodes in the world.
     * Returns a map of nodeId to computed [NodeDelta].
     */
    fun tick(dt: Float = 0.016f): Map<String, NodeDelta> {
        val deltas = mutableMapOf<String, NodeDelta>()
        val allNodes = quadTreeEngine.getAllNodes()

        for (node in allNodes) {
            // Fixed obstacles and dragged nodes don't obey autonomous spring dynamics
            if (node.isFixedObstacle) {
                deltas[node.id] = NodeDelta(
                    offsetX = node.x - node.anchorX,
                    offsetY = node.y - node.anchorY
                )
                continue
            }

            if (node.isBeingDragged) {
                node.vx = 0f
                node.vy = 0f
                node.isSleeping = false
                deltas[node.id] = NodeDelta(
                    offsetX = node.x - node.anchorX,
                    offsetY = node.y - node.anchorY,
                    scaleX = 1.05f,
                    scaleY = 1.05f
                )
                continue
            }

            // Calculate spring force toward anchor: F = -k * dx
            val dx = node.x - node.anchorX
            val dy = node.y - node.anchorY
            var fx = -springStiffness * dx
            var fy = -springStiffness * dy

            // Apply damping: F = -zeta * v
            fx -= dampingCoefficient * node.vx
            fy -= dampingCoefficient * node.vy

            // Query spatial neighbors for collision repulsion
            val neighbors = quadTreeEngine.queryNeighbors(node, collisionMargin)
            for (neighbor in neighbors) {
                if (node.overlaps(neighbor)) {
                    val impulse = CollisionResolver.resolveSeparationImpulse(
                        a = node,
                        b = neighbor,
                        separationStrength = separationStrength
                    )
                    fx += impulse.x
                    fy += impulse.y
                    node.isSleeping = false
                }
            }

            // Semi-implicit Euler integration: v = v + F * dt, x = x + v * dt
            node.vx += fx * dt
            node.vy += fy * dt
            node.x += node.vx * dt
            node.y += node.vy * dt

            // Sleep detection
            if (abs(node.vx) < sleepThresholdVelocity &&
                abs(node.vy) < sleepThresholdVelocity &&
                abs(dx) < positionEpsilon &&
                abs(dy) < positionEpsilon
            ) {
                node.x = node.anchorX
                node.y = node.anchorY
                node.vx = 0f
                node.vy = 0f
                node.isSleeping = true
            }

            deltas[node.id] = NodeDelta(
                offsetX = node.x - node.anchorX,
                offsetY = node.y - node.anchorY,
                scaleX = 1f,
                scaleY = 1f,
                rotationZ = (node.vx * 0.02f).coerceIn(-15f, 15f),
                alpha = 1f
            )
        }

        return deltas
    }
}
