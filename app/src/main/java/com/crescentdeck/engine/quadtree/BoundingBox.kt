package com.crescentdeck.engine.quadtree

/**
 * 2D Axis-Aligned Bounding Box (AABB) representation for spatial indexing.
 */
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    val left: Float get() = x
    val top: Float get() = y
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val centerX: Float get() = x + (width / 2f)
    val centerY: Float get() = y + (height / 2f)

    /**
     * Checks if this bounding box intersects with [other].
     */
    fun intersects(other: BoundingBox): Boolean {
        return this.left < other.right &&
                this.right > other.left &&
                this.top < other.bottom &&
                this.bottom > other.top
    }

    /**
     * Checks if this bounding box completely encloses [other].
     */
    fun contains(other: BoundingBox): Boolean {
        return other.left >= this.left &&
                other.right <= this.right &&
                other.top >= this.top &&
                other.bottom <= this.bottom
    }

    /**
     * Checks if a point (px, py) is inside this bounding box.
     */
    fun containsPoint(px: Float, py: Float): Boolean {
        return px in left..right && py in top..bottom
    }

    /**
     * Returns an expanded bounding box with added margin on all sides.
     */
    fun expanded(margin: Float): BoundingBox {
        return BoundingBox(
            x = x - margin,
            y = y - margin,
            width = width + (2 * margin),
            height = height + (2 * margin)
        )
    }
}
