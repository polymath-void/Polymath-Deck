package com.polymathdeck.engine.governor

import javax.inject.Inject
import javax.inject.Singleton

enum class MemoryState {
    GREEN,
    YELLOW,
    RED
}

/**
 * Monitors runtime heap allocation and memory pressure.
 */
@Singleton
class MemoryPressureMonitor @Inject constructor() {

    var isMediaPlaybackActive: Boolean = false

    fun getMemoryState(): MemoryState {
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()).toFloat()
        val max = runtime.maxMemory().toFloat()
        val ratio = used / max

        val greenLimit = if (isMediaPlaybackActive) 0.45f else 0.60f
        val yellowLimit = if (isMediaPlaybackActive) 0.70f else 0.80f

        return when {
            ratio < greenLimit -> MemoryState.GREEN
            ratio < yellowLimit -> MemoryState.YELLOW
            else -> MemoryState.RED
        }
    }

    fun getUsedMemoryRatio(): Float {
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()).toFloat()
        val max = runtime.maxMemory().toFloat()
        return used / max
    }
}
