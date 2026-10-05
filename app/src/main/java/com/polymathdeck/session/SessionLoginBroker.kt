package com.polymathdeck.session

import android.webkit.CookieManager
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ensures user login sessions and authentication cookies persist seamlessly
 * across application restarts, card hibernation cycles, and WebView recycling.
 */
@Singleton
class SessionLoginBroker @Inject constructor() {

    private val cookieManager: CookieManager
        get() = CookieManager.getInstance()

    init {
        // Ensure global cookie acceptance is enabled
        cookieManager.setAcceptCookie(true)
    }

    /**
     * Configures a live WebView instance with persistent cookie handling,
     * including third-party cookies required for federated OAuth (e.g. Google, GitHub login).
     */
    fun configureWebView(webView: WebView) {
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
    }

    /**
     * Asynchronously flushes all pending memory cookies to disk storage.
     */
    suspend fun flushCookies() = withContext(Dispatchers.IO) {
        cookieManager.flush()
    }

    /**
     * Synchronously flushes cookies when an immediate flush is required (e.g. onPause / onStop).
     */
    fun flushSync() {
        cookieManager.flush()
    }

    /**
     * Retrieves stored cookies for a given URL domain.
     */
    fun getCookies(url: String): String? {
        return cookieManager.getCookie(url)
    }

    /**
     * Sets or updates a cookie for a specific URL domain.
     */
    fun setCookie(url: String, cookieString: String) {
        cookieManager.setCookie(url, cookieString)
    }

    /**
     * Clears all session cookies if requested.
     */
    fun clearSessionCookies(onComplete: (() -> Unit)? = null) {
        cookieManager.removeSessionCookies {
            cookieManager.flush()
            onComplete?.invoke()
        }
    }
}
