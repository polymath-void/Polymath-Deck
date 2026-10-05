package com.polymathdeck.engine.quadtree

/**
 * Mutable spatial representation of a card or obstacle in 2D space.
 */
data class CardNode(
    val id: String,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var anchorX: Float,
    var anchorY: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var priority: Int = 1,
    var cardType: Int = 0,
    var isSleeping: Boolean = false,
    var isFixedObstacle: Boolean = false,
    var isBeingDragged: Boolean = false,
    var isDetached: Boolean = false,
    var lifecycleState: Int = 0
) {
    val boundingBox: BoundingBox
        get() = BoundingBox(x, y, width, height)

    val anchorBoundingBox: BoundingBox
        get() = BoundingBox(anchorX, anchorY, width, height)

    fun overlaps(other: CardNode): Boolean = boundingBox.intersects(other.boundingBox)

    fun expandedBounds(margin: Float): BoundingBox = boundingBox.expanded(margin)
}

/**
 * Node within the recursive QuadTree spatial partitioning hierarchy.
 */
class QuadTreePartition(
    val bounds: BoundingBox,
    val depth: Int = 0,
    private val maxDepth: Int = 8,
    private val threshold: Int = 4
) {
    val nodes: MutableList<CardNode> = mutableListOf()
    var northWest: QuadTreePartition? = null
    var northEast: QuadTreePartition? = null
    var southWest: QuadTreePartition? = null
    var southEast: QuadTreePartition? = null

    val isDivided: Boolean get() = northWest != null

    /**
     * Subdivides this quadrant into 4 equal child quadrants.
     */
    fun subdivide() {
        val halfW = bounds.width / 2f
        val halfH = bounds.height / 2f
        val nextDepth = depth + 1

        northWest = QuadTreePartition(BoundingBox(bounds.x, bounds.y, halfW, halfH), nextDepth, maxDepth, threshold)
        northEast = QuadTreePartition(BoundingBox(bounds.x + halfW, bounds.y, halfW, halfH), nextDepth, maxDepth, threshold)
        southWest = QuadTreePartition(BoundingBox(bounds.x, bounds.y + halfH, halfW, halfH), nextDepth, maxDepth, threshold)
        southEast = QuadTreePartition(BoundingBox(bounds.x + halfW, bounds.y + halfH, halfW, halfH), nextDepth, maxDepth, threshold)
    }

    /**
     * Inserts a [node] into this partition or down to its child quadrants.
     */
    fun insert(node: CardNode): Boolean {
        if (!bounds.intersects(node.boundingBox)) {
            return false
        }

        if (!isDivided) {
            if (nodes.size < threshold || depth >= maxDepth) {
                nodes.add(node)
                return true
            }
            subdivide()

            // Push existing nodes down where possible
            val iterator = nodes.iterator()
            while (iterator.hasNext()) {
                val existing = iterator.next()
                if (pushDown(existing)) {
                    iterator.remove()
                }
            }
        }

        // Try pushing new node down
        if (pushDown(node)) {
            return true
        }

        // Fallback: node spans quadrant boundaries, keep at this parent level
        nodes.add(node)
        return true
    }

    private fun pushDown(node: CardNode): Boolean {
        val nw = northWest ?: return false
        val ne = northEast ?: return false
        val sw = southWest ?: return false
        val se = southEast ?: return false

        if (nw.bounds.contains(node.boundingBox)) return nw.insert(node)
        if (ne.bounds.contains(node.boundingBox)) return ne.insert(node)
        if (sw.bounds.contains(node.boundingBox)) return sw.insert(node)
        if (se.bounds.contains(node.boundingBox)) return se.insert(node)

        return false
    }

    /**
     * Queries all nodes intersecting with [viewport].
     */
    fun queryRange(viewport: BoundingBox, results: MutableList<CardNode>) {
        if (!bounds.intersects(viewport)) {
            return
        }

        for (node in nodes) {
            if (!node.isDetached && node.boundingBox.intersects(viewport)) {
                results.add(node)
            }
        }

        northWest?.queryRange(viewport, results)
        northEast?.queryRange(viewport, results)
        southWest?.queryRange(viewport, results)
        southEast?.queryRange(viewport, results)
    }

    /**
     * Clears all subtrees and nodes.
     */
    fun clear() {
        nodes.clear()
        northWest?.clear()
        northEast?.clear()
        southWest?.clear()
        southEast?.clear()
        northWest = null
        northEast = null
        southWest = null
        southEast = null
    }
}
