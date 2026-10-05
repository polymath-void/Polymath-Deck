package com.crescentdeck.ui.card

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.crescentdeck.data.db.entity.CardEntity
import com.crescentdeck.engine.governor.WebViewResourceGovernor
import com.crescentdeck.ui.theme.ThemeManager

/**
 * Hosts an active, sandboxed Android WebView inside a spatial card container.
 * Dynamically injects theme CSS variables, handles URL interception to spawn new nodes,
 * and adheres to Governor memory throttling.
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
        if (card.contentPayload.startsWith("http")) card.contentPayload else "https://en.wikipedia.org"
    }

    DisposableEffect(card.cardId) {
        onDispose {
            governor.unregisterWebView(card.cardId)
        }
    }

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
                        val url = request?.url?.toString() ?: return false
                        return handleUrlInterception(url)
                    }

                    @Suppress("DEPRECATION")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (url == null) return false
                        return handleUrlInterception(url)
                    }

                    private fun handleUrlInterception(url: String): Boolean {
                        if (url == targetUrl) return false
                        if (onOpenUrlAsCard != null) {
                            onOpenUrlAsCard(url)
                            return true // Intercept: keep current card intact, spawn new card node on canvas
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
                                var style = document.getElementById('crescent-theme-style');
                                if (!style) {
                                    style = document.createElement('style');
                                    style.id = 'crescent-theme-style';
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
            }
        },
        update = { webView ->
            if (webView.url != targetUrl && !card.isHibernated) {
                webView.loadUrl(targetUrl)
            }
        }
    )
}
