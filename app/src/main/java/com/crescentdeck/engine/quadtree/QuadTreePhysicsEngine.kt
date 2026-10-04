package com.crescentdeck.engine.quadtree

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 2D Spatial coordinate system and QuadTree spatial partitioning engine.
 * Maintains O(log N) bounding box queries and rapid neighbor collision detection.
 */
@Singleton
class QuadTreePhysicsEngine @Inject constructor() {

    var worldBounds: BoundingBox = BoundingBox(-25000f, -25000f, 50000f, 50000f)
        private set

    private var root: QuadTreePartition = QuadTreePartition(worldBounds, depth = 0)
    private val allNodes = ConcurrentHashMap<String, CardNode>()
    private var dirtyNodeCount: Int = 0

    private val treeLock = Any()

    /**
     * Initializes or resets the world coordinate bounds.
     */
    fun setBounds(bounds: BoundingBox) {
        synchronized(treeLock) {
            worldBounds = bounds
            rebuildTreeInternal()
        }
    }

    /**
     * Inserts or replaces a node in the physics world.
     */
    fun insertNode(node: CardNode) {
        synchronized(treeLock) {
            allNodes[node.id] = node
            dirtyNodeCount++
            checkRebuildNeeded()
        }
    }

    /**
     * Batch inserts multiple nodes.
     */
    fun batchInsert(nodes: List<CardNode>) {
        synchronized(treeLock) {
            for (node in nodes) {
                allNodes[node.id] = node
            }
            rebuildTreeInternal()
        }
    }

    /**
     * Updates an existing node's location.
     */
    fun updateNodePosition(nodeId: String, x: Float, y: Float) {
        val node = allNodes[nodeId] ?: return
        node.x = x
        node.y = y
        synchronized(treeLock) {
            dirtyNodeCount++
            checkRebuildNeeded()
        }
    }

    /**
     * Removes a node by ID.
     */
    fun removeNode(nodeId: String): CardNode? {
        synchronized(treeLock) {
            val removed = allNodes.remove(nodeId)
            if (removed != null) {
                dirtyNodeCount++
                checkRebuildNeeded()
            }
            return removed
        }
    }

    /**
     * Locates a node in O(1) memory index.
     */
    fun locateNode(nodeId: String): CardNode? = allNodes[nodeId]

    /**
     * Returns a snapshot list of all tracked nodes.
     */
    fun getAllNodes(): List<CardNode> = allNodes.values.toList()

    /**
     * Queries all nodes intersecting with the specified [viewport].
     */
    fun queryRange(viewport: BoundingBox): List<CardNode> {
        val results = mutableListOf<CardNode>()
        synchronized(treeLock) {
            root.queryRange(viewport, results)
        }
        return results
    }

    /**
     * Queries neighboring nodes within [margin] distance around [node].
     */
    fun queryNeighbors(node: CardNode, margin: Float = 8f): List<CardNode> {
        val searchArea = node.expandedBounds(margin)
        return queryRange(searchArea).filter { it.id != node.id }
    }

    /**
     * Triggers a tree rebuild if dirty mutations exceed 30% of total nodes.
     */
    private fun checkRebuildNeeded() {
        val total = allNodes.size
        if (total > 0 && (dirtyNodeCount.toFloat() / total.toFloat()) > 0.30f) {
            rebuildTreeInternal()
        }
    }

    /**
     * Rebuilds the QuadTree partition hierarchy from scratch.
     */
    fun forceRebuild() {
        synchronized(treeLock) {
            rebuildTreeInternal()
        }
    }

    private fun rebuildTreeInternal() {
        root.clear()
        root = QuadTreePartition(worldBounds, depth = 0)
        for (node in allNodes.values) {
            root.insert(node)
        }
        dirtyNodeCount = 0
    }

    /**
     * Clears all state.
     */
    fun clear() {
        synchronized(treeLock) {
            allNodes.clear()
            root.clear()
            root = QuadTreePartition(worldBounds, depth = 0)
            dirtyNodeCount = 0
        }
    }
}
