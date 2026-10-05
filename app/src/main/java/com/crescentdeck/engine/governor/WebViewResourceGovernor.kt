package com.crescentdeck.engine.governor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView
import com.crescentdeck.engine.quadtree.QuadTreePhysicsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class CardResourceState {
    ACTIVE,
    THROTTLED,
    HIBERNATED
}

/**
 * 4-Tier Lifecycle state machine for spatial cards:
 * - GRID_FLOW: Visible on 2D canvas, active WebView process, attached in QuadTree, normal priority
 * - IMMERSIVE: Fullscreen view, active WebView process, detached from QuadTree, high priority (immune to LRU eviction)
 * - MINIMIZED: Rendered as icon/pill, WebView process terminated (rendered via snapshot bitmap scrim), detached from QuadTree, RAM saved
 * - RESIZING: Visible on canvas, active WebView process, attached with propagating repulsion to neighboring cards, high priority
 */
enum class CardLifecycleState {
    GRID_FLOW,
    IMMERSIVE,
    MINIMIZED,
    RESIZING
}

/**
 * Manages allocation, throttling, and hibernation of heavy WebView instances
 * to prevent Android Low Memory Killer (LMK) process termination.
 *
 * Implements a strict LRU watchdog capped at [maxActiveWebViews] (default 6).
 * When opening a 7th active card, automatically triggers LRU eviction to minimize
 * the oldest active card in GRID_FLOW.
 */
@Singleton
class WebViewResourceGovernor @Inject constructor(
    private val memoryMonitor: MemoryPressureMonitor
) {

    companion object {
        const val DEFAULT_MAX_ACTIVE_WEBVIEWS = 6
    }

    var maxActiveWebViews: Int = DEFAULT_MAX_ACTIVE_WEBVIEWS
    var quadTreeEngine: QuadTreePhysicsEngine? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val trackedViews = ConcurrentHashMap<String, WeakReference<WebView>>()
    private val cardStates = ConcurrentHashMap<String, CardResourceState>()
    private val lifecycleStates = ConcurrentHashMap<String, CardLifecycleState>()
    private val accessTimestamps = ConcurrentHashMap<String, Long>()
    private val snapshotBitmaps = ConcurrentHashMap<String, Bitmap>()

    private val _governorEvents = MutableStateFlow<Map<String, CardResourceState>>(emptyMap())
    val governorEvents: StateFlow<Map<String, CardResourceState>> = _governorEvents.asStateFlow()

    private val _lifecycleEvents = MutableStateFlow<Map<String, CardLifecycleState>>(emptyMap())
    val lifecycleEvents: StateFlow<Map<String, CardLifecycleState>> = _lifecycleEvents.asStateFlow()

    private val _floatingCards = MutableStateFlow<Set<String>>(emptySet())
    val floatingCards: StateFlow<Set<String>> = _floatingCards.asStateFlow()

    /**
     * Updates the LRU access timestamp for [cardId].
     */
    fun touchCard(cardId: String) {
        accessTimestamps[cardId] = System.currentTimeMillis()
    }

    /**
     * Registers an active WebView instance under resource management.
     */
    fun registerWebView(cardId: String, webView: WebView) {
        trackedViews[cardId] = WeakReference(webView)
        cardStates[cardId] = CardResourceState.ACTIVE
        lifecycleStates[cardId] = CardLifecycleState.GRID_FLOW
        touchCard(cardId)
        enforceLRULimit(exemptCardId = cardId)
        evaluateMemoryPressure()
        emitStateChange()
    }

    /**
     * Unregisters a WebView from the governor.
     */
    fun unregisterWebView(cardId: String) {
        trackedViews.remove(cardId)
        cardStates.remove(cardId)
        lifecycleStates.remove(cardId)
        accessTimestamps.remove(cardId)
        snapshotBitmaps.remove(cardId)?.recycle()
        _floatingCards.value = _floatingCards.value - cardId
        emitStateChange()
    }

    /**
     * Captures a visual snapshot bitmap of the card before hibernation/minimization.
     */
    fun captureSnapshot(cardId: String, webView: WebView): Bitmap? {
        return try {
            if (webView.width <= 0 || webView.height <= 0) return null
            val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)
            snapshotBitmaps[cardId]?.recycle()
            snapshotBitmaps[cardId] = bitmap
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun getSnapshot(cardId: String): Bitmap? = snapshotBitmaps[cardId]

    /**
     * Transitions [cardId] to [targetState] following the 4-tier lifecycle state machine.
     */
    fun transitionTo(cardId: String, targetState: CardLifecycleState) {
        touchCard(cardId)
        lifecycleStates[cardId] = targetState

        when (targetState) {
            CardLifecycleState.GRID_FLOW -> {
                cardStates[cardId] = CardResourceState.ACTIVE
                quadTreeEngine?.locateNode(cardId)?.let { node ->
                    node.isDetached = false
                    node.isSleeping = false
                }
                enforceLRULimit(exemptCardId = cardId)
            }
            CardLifecycleState.IMMERSIVE -> {
                cardStates[cardId] = CardResourceState.ACTIVE
                // Detach from QuadTree so grid cards don't collide with fullscreen overlay
                quadTreeEngine?.locateNode(cardId)?.let { node ->
                    node.isDetached = true
                }
            }
            CardLifecycleState.MINIMIZED -> {
                cardStates[cardId] = CardResourceState.HIBERNATED
                quadTreeEngine?.locateNode(cardId)?.let { node ->
                    node.isDetached = true
                }
                hibernateInternal(cardId)
            }
            CardLifecycleState.RESIZING -> {
                cardStates[cardId] = CardResourceState.ACTIVE
                quadTreeEngine?.locateNode(cardId)?.let { node ->
                    node.isDetached = false
                    node.priority = 10 // Higher priority to propagate repulsion
                    node.isSleeping = false
                }
            }
        }
        emitStateChange()
    }

    /**
     * Detaches card from spatial QuadTree to FloatingStateFlow for Picture-in-Picture playback.
     */
    fun detachToPiP(cardId: String) {
        _floatingCards.value = _floatingCards.value + cardId
        transitionTo(cardId, CardLifecycleState.IMMERSIVE)
    }

    /**
     * Re-attaches card from Picture-in-Picture back to the 2D spatial canvas.
     */
    fun attachFromPiP(cardId: String) {
        _floatingCards.value = _floatingCards.value - cardId
        transitionTo(cardId, CardLifecycleState.GRID_FLOW)
    }

    /**
     * Enforces the LRU watchdog limit (default 6 active WebViews).
     * If active count exceeds [maxActiveWebViews], minimizes the oldest GRID_FLOW card.
     */
    fun enforceLRULimit(exemptCardId: String? = null) {
        val activeIds = cardStates.filter {
            it.value == CardResourceState.ACTIVE &&
                    lifecycleStates[it.key] != CardLifecycleState.IMMERSIVE
        }.keys.toList()

        if (activeIds.size > maxActiveWebViews) {
            val toEvict = activeIds
                .filter { it != exemptCardId }
                .minByOrNull { accessTimestamps[it] ?: 0L }

            if (toEvict != null) {
                transitionTo(toEvict, CardLifecycleState.MINIMIZED)
            }
        }
    }

    /**
     * Explicitly hibernates a card's WebView to reclaim native memory.
     */
    fun hibernateCard(cardId: String) {
        transitionTo(cardId, CardLifecycleState.MINIMIZED)
    }

    private fun hibernateInternal(cardId: String) {
        val webViewRef = trackedViews[cardId] ?: return
        val webView = webViewRef.get()

        mainHandler.post {
            if (webView != null) {
                captureSnapshot(cardId, webView)
                webView.onPause()
                webView.pauseTimers()
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.destroy()
            }
            trackedViews.remove(cardId)
            cardStates[cardId] = CardResourceState.HIBERNATED
            emitStateChange()
        }
    }

    /**
     * Restores a card from hibernation into active GRID_FLOW state.
     */
    fun wakeCard(cardId: String) {
        transitionTo(cardId, CardLifecycleState.GRID_FLOW)
    }

    /**
     * Evaluates current system memory and applies progressive throttling or hibernation.
     */
    fun evaluateMemoryPressure() {
        val pressure = memoryMonitor.getMemoryState()

        when (pressure) {
            MemoryState.GREEN -> {
                for ((cardId, state) in cardStates) {
                    if (state == CardResourceState.THROTTLED) {
                        cardStates[cardId] = CardResourceState.ACTIVE
                        trackedViews[cardId]?.get()?.resumeTimers()
                    }
                }
            }
            MemoryState.YELLOW -> {
                for ((cardId, state) in cardStates) {
                    if (state == CardResourceState.ACTIVE && lifecycleStates[cardId] != CardLifecycleState.IMMERSIVE) {
                        cardStates[cardId] = CardResourceState.THROTTLED
                        trackedViews[cardId]?.get()?.pauseTimers()
                    }
                }
            }
            MemoryState.RED -> {
                val toHibernate = cardStates
                    .filter { it.value != CardResourceState.HIBERNATED && lifecycleStates[it.key] != CardLifecycleState.IMMERSIVE }
                    .keys
                    .sortedBy { accessTimestamps[it] ?: 0L }
                    .take(3)
                for (id in toHibernate) {
                    transitionTo(id, CardLifecycleState.MINIMIZED)
                }
            }
        }
        emitStateChange()
    }

    private fun emitStateChange() {
        _governorEvents.value = cardStates.toMap()
        _lifecycleEvents.value = lifecycleStates.toMap()
    }
}
