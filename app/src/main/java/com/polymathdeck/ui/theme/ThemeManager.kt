package com.polymathdeck.ui.theme

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

    private val _currentGlobalTheme = MutableStateFlow(PolymathTheme.DefaultDark)
    val currentGlobalTheme: StateFlow<PolymathTheme> = _currentGlobalTheme.asStateFlow()

    private val categoryOverrides = ConcurrentHashMap<String, Map<String, String>>()
    private val deckOverrides = ConcurrentHashMap<String, Map<String, String>>()
    private val cardOverrides = ConcurrentHashMap<String, Map<String, String>>()

    private val _themeEvents = MutableSharedFlow<ThemeChangeEvent>(extraBufferCapacity = 32)
    val themeEvents: SharedFlow<ThemeChangeEvent> = _themeEvents.asSharedFlow()

    fun setGlobalTheme(theme: PolymathTheme) {
        _currentGlobalTheme.value = theme
        _themeEvents.tryEmit(ThemeChangeEvent.GlobalChange(theme))
    }

    fun setCategoryOverride(category: String, overrides: Map<String, String>) {
        categoryOverrides[category] = overrides
        _themeEvents.tryEmit(ThemeChangeEvent.CategoryOverride(category, overrides))
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
     * Evaluates cascade hierarchy: Global (base) -> Category Overrides -> Deck Overrides -> Card Overrides.
     */
    fun getComputedCssForCard(deckId: String, cardId: String, category: String = "general"): String {
        val baseCss = _currentGlobalTheme.value.toCssVariables()
        val catRules = categoryOverrides[category] ?: emptyMap()
        val deckRules = deckOverrides[deckId] ?: emptyMap()
        val cardRules = cardOverrides[cardId] ?: emptyMap()

        if (catRules.isEmpty() && deckRules.isEmpty() && cardRules.isEmpty()) {
            return baseCss
        }

        val merged = mutableMapOf<String, String>()
        for ((k, v) in catRules) merged[k] = v
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
