package com.polymathdeck.ui.card

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.polymathdeck.data.db.entity.CardEntity
import com.polymathdeck.engine.governor.WebViewResourceGovernor
import com.polymathdeck.ui.theme.ThemeManager

/**
 * Hosts an active, sandboxed Android WebView inside a spatial card container.
 * Dynamically injects theme CSS variables, safely intercepts external link taps to spawn new nodes,
 * and adheres to Governor memory throttling without runaway reload/redirect loops.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveCardFrame(
    card: CardEntity,
    governor: WebViewResourceGovernor,
    themeManager: ThemeManager,
    onOpenUrlAsCard: ((url: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targetUrl = remember(card.contentPayload) {
        val payload = card.contentPayload.trim()
        if (payload.startsWith("http://", ignoreCase = true) || payload.startsWith("https://", ignoreCase = true)) {
            payload
        } else if (payload.isNotEmpty()) {
            "https://$payload"
        } else {
            "https://en.wikipedia.org"
        }
    }

    DisposableEffect(card.cardId) {
        onDispose {
            governor.unregisterWebView(card.cardId)
        }
    }

    var loadedTargetUrl by remember { mutableStateOf("") }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(AndroidColor.TRANSPARENT)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString() ?: return false

                        // 1. NEVER intercept server-side redirects or subframe/iframe/script/asset requests
                        if (request.isRedirect || !request.isForMainFrame) {
                            return false
                        }

                        // 2. Only explicit user gestures (taps on links) can trigger external card creation
                        if (!request.hasGesture()) {
                            return false
                        }

                        return handleUrlInterception(requestUrl)
                    }

                    @Suppress("DEPRECATION")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }

                    private fun handleUrlInterception(url: String): Boolean {
                        if (url.equals(targetUrl, ignoreCase = true)) return false

                        // Compare domain hosts to distinguish internal navigation from external links
                        val targetHost = try { Uri.parse(targetUrl).host?.lowercase() } catch (e: Exception) { null }
                        val incomingHost = try { Uri.parse(url).host?.lowercase() } catch (e: Exception) { null }

                        if (targetHost != null && incomingHost != null) {
                            val isSameDomain = targetHost == incomingHost ||
                                    targetHost.endsWith(".$incomingHost") ||
                                    incomingHost.endsWith(".$targetHost")
                            if (isSameDomain) {
                                return false // Allow user to navigate freely inside the current site
                            }
                        }

                        // User explicitly clicked an external link: spawn new card on canvas
                        if (onOpenUrlAsCard != null) {
                            onOpenUrlAsCard(url)
                            return true // Intercept: keep current card intact, spawn new node on canvas
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject QuadTreeLayoutNormalizer and 3-level cascade theme CSS
                        val normalizerCss = """
                            html, body {
                                overflow-x: hidden !important;
                                overscroll-behavior: contain !important;
                                touch-action: pan-y pinch-zoom !important;
                                -webkit-overflow-scrolling: touch !important;
                                max-width: 100% !important;
                                box-sizing: border-box !important;
                            }
                            * {
                                box-sizing: border-box !important;
                            }
                            ::-webkit-scrollbar {
                                width: 4px;
                                height: 4px;
                            }
                            ::-webkit-scrollbar-thumb {
                                background: rgba(56, 189, 248, 0.4);
                                border-radius: 4px;
                            }
                            ::-webkit-scrollbar-track {
                                background: transparent;
                            }
                        """.trimIndent()
                        val computedThemeCss = themeManager.getComputedCssForCard(card.deckId, card.cardId)
                        val fullCss = "$normalizerCss\n$computedThemeCss"
                        val encodedCss = android.util.Base64.encodeToString(
                            fullCss.toByteArray(), android.util.Base64.NO_WRAP
                        )
                        val js = """
                            (function() {
                                var parent = document.head || document.documentElement;
                                var style = document.getElementById('polymath-theme-style');
                                if (!style) {
                                    style = document.createElement('style');
                                    style.id = 'polymath-theme-style';
                                    parent.appendChild(style);
                                }
                                style.textContent = atob('$encodedCss');
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(js, null)
                    }
                }
                webChromeClient = WebChromeClient()

                // Gesture Segregation: allow internal page vertical scrolling without interference
                setOnTouchListener { v, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN,
                        android.view.MotionEvent.ACTION_MOVE -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }

                governor.registerWebView(card.cardId, this)
                loadUrl(targetUrl)
                loadedTargetUrl = targetUrl
            }
        },
        update = { webView ->
            if (loadedTargetUrl != targetUrl && !card.isHibernated) {
                loadedTargetUrl = targetUrl
                webView.loadUrl(targetUrl)
            }
        }
    )
}
