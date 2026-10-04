package com.crescentdeck.ui.card

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebChromeClient
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
 * Dynamically injects theme CSS variables and adheres to Governor memory throttling.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveCardFrame(
    card: CardEntity,
    governor: WebViewResourceGovernor,
    themeManager: ThemeManager,
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
                    cacheMode = WebSettings.LOAD_DEFAULT
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject computed theme CSS
                        val css = themeManager.getComputedCssForCard(card.deckId, card.cardId)
                        val encodedCss = android.util.Base64.encodeToString(
                            css.toByteArray(), android.util.Base64.NO_WRAP
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
