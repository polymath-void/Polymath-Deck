package com.crescentdeck.ui.theme

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 3-Level Theme Cascade Manager.
 * Computes resolved styling with priority: Global (base) -> Deck Overrides -> Card Overrides.
 */
@Singleton
class ThemeManager @Inject constructor() {

    private val _currentGlobalTheme = MutableStateFlow(CrescentTheme.DefaultDark)
    val currentGlobalTheme: StateFlow<CrescentTheme> = _currentGlobalTheme.asStateFlow()

    private val deckOverrides = ConcurrentHashMap<String, Map<String, String>>()
    private val cardOverrides = ConcurrentHashMap<String, Map<String, String>>()

    private val _themeEvents = MutableSharedFlow<ThemeChangeEvent>(extraBufferCapacity = 32)
    val themeEvents: SharedFlow<ThemeChangeEvent> = _themeEvents.asSharedFlow()

    fun setGlobalTheme(theme: CrescentTheme) {
        _currentGlobalTheme.value = theme
        _themeEvents.tryEmit(ThemeChangeEvent.GlobalChange(theme))
    }

    fun setDeckOverride(deckId: String, overrides: Map<String, String>) {
        deckOverrides[deckId] = overrides
        _themeEvents.tryEmit(ThemeChangeEvent.DeckOverride(deckId, overrides))
    }

    fun setCardOverride(cardId: String, cssVars: Map<String, String>) {
        cardOverrides[cardId] = cssVars
        _themeEvents.tryEmit(ThemeChangeEvent.CardOverride(cardId, cssVars))
    }

    /**
     * Resolves the combined CSS string to inject into card WebViews.
     */
    fun getComputedCssForCard(deckId: String, cardId: String): String {
        val baseCss = _currentGlobalTheme.value.toCssVariables()
        val deckRules = deckOverrides[deckId] ?: emptyMap()
        val cardRules = cardOverrides[cardId] ?: emptyMap()

        if (deckRules.isEmpty() && cardRules.isEmpty()) {
            return baseCss
        }

        val merged = mutableMapOf<String, String>()
        for ((k, v) in deckRules) merged[k] = v
        for ((k, v) in cardRules) merged[k] = v

        val customCss = merged.entries.joinToString("\n") { (k, v) -> "  $k: $v;" }

        return """
            $baseCss
            :root {
            $customCss
            }
        """.trimIndent()
    }
}
