package com.crescentdeck.engine.governor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView
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
 * Manages allocation, throttling, and hibernation of heavy WebView instances
 * to prevent Android Low Memory Killer (LMK) process termination.
 */
@Singleton
class WebViewResourceGovernor @Inject constructor(
    private val memoryMonitor: MemoryPressureMonitor
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val trackedViews = ConcurrentHashMap<String, WeakReference<WebView>>()
    private val cardStates = ConcurrentHashMap<String, CardResourceState>()
    private val snapshotBitmaps = ConcurrentHashMap<String, Bitmap>()

    private val _governorEvents = MutableStateFlow<Map<String, CardResourceState>>(emptyMap())
    val governorEvents: StateFlow<Map<String, CardResourceState>> = _governorEvents.asStateFlow()

    /**
     * Registers an active WebView instance under resource management.
     */
    fun registerWebView(cardId: String, webView: WebView) {
        trackedViews[cardId] = WeakReference(webView)
        cardStates[cardId] = CardResourceState.ACTIVE
        evaluateMemoryPressure()
    }

    /**
     * Unregisters a WebView from the governor.
     */
    fun unregisterWebView(cardId: String) {
        trackedViews.remove(cardId)
        cardStates.remove(cardId)
        snapshotBitmaps.remove(cardId)?.recycle()
        emitStateChange()
    }

    /**
     * Captures a visual snapshot bitmap of the card before hibernation.
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
     * Explicitly hibernates a card's WebView to reclaim native memory.
     */
    fun hibernateCard(cardId: String) {
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
     * Restores a card from hibernation into active state.
     */
    fun wakeCard(cardId: String) {
        cardStates[cardId] = CardResourceState.ACTIVE
        emitStateChange()
    }

    /**
     * Evaluates current system memory and applies progressive throttling or hibernation.
     */
    fun evaluateMemoryPressure() {
        val pressure = memoryMonitor.getMemoryState()

        when (pressure) {
            MemoryState.GREEN -> {
                // Resume timers on throttled views
                for ((cardId, state) in cardStates) {
                    if (state == CardResourceState.THROTTLED) {
                        cardStates[cardId] = CardResourceState.ACTIVE
                        trackedViews[cardId]?.get()?.resumeTimers()
                    }
                }
            }
            MemoryState.YELLOW -> {
                // Throttle background views
                for ((cardId, state) in cardStates) {
                    if (state == CardResourceState.ACTIVE) {
                        cardStates[cardId] = CardResourceState.THROTTLED
                        trackedViews[cardId]?.get()?.pauseTimers()
                    }
                }
            }
            MemoryState.RED -> {
                // Aggressive hibernation: hibernate oldest active/throttled views
                val toHibernate = cardStates.filter { it.value != CardResourceState.HIBERNATED }.keys.take(3)
                for (id in toHibernate) {
                    hibernateCard(id)
                }
            }
        }
        emitStateChange()
    }

    private fun emitStateChange() {
        _governorEvents.value = cardStates.toMap()
    }
}
