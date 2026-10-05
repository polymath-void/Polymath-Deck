package com.polymathdeck.ui.theme

/**
 * Event payload broadcast to observers when theme styles change at any cascade level.
 */
sealed class ThemeChangeEvent {
    data class GlobalChange(val theme: PolymathTheme) : ThemeChangeEvent()
    data class CategoryOverride(val category: String, val overrides: Map<String, String>) : ThemeChangeEvent()
    data class DeckOverride(val deckId: String, val overrides: Map<String, String>) : ThemeChangeEvent()
    data class CardOverride(val cardId: String, val cssVars: Map<String, String>) : ThemeChangeEvent()
}
