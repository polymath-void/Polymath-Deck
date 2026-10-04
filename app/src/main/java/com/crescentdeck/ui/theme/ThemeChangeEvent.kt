package com.crescentdeck.ui.theme

/**
 * Event payload broadcast to observers when theme styles change at any cascade level.
 */
sealed class ThemeChangeEvent {
    data class GlobalChange(val theme: CrescentTheme) : ThemeChangeEvent()
    data class DeckOverride(val deckId: String, val overrides: Map<String, String>) : ThemeChangeEvent()
    data class CardOverride(val cardId: String, val cssVars: Map<String, String>) : ThemeChangeEvent()
}
