package com.polymathdeck.theme

import com.polymathdeck.ui.theme.PolymathTheme
import com.polymathdeck.ui.theme.ThemeManager
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ThemeManagerTest {

    private lateinit var themeManager: ThemeManager

    @Before
    fun setUp() {
        themeManager = ThemeManager()
    }

    @Test
    fun testGlobalThemeGeneratesCssVariables() {
        themeManager.setGlobalTheme(PolymathTheme.DefaultDark)
        val css = themeManager.getComputedCssForCard("deck_1", "card_1")

        assertTrue(css.contains("--polymath-bg:"))
        assertTrue(css.contains("--polymath-accent:"))
    }

    @Test
    fun testCategoryAndCardCascadeOverrides() {
        themeManager.setGlobalTheme(PolymathTheme.DefaultDark)

        // Category override
        themeManager.setCategoryOverride("media", mapOf("--polymath-accent" to "#ff007f"))

        // Deck override
        themeManager.setDeckOverride("deck_1", mapOf("--polymath-surface" to "#112233"))

        // Card override
        themeManager.setCardOverride("card_special", mapOf("--polymath-accent" to "#00ffcc"))

        // Check card without card-level override gets category and deck overrides
        val cssGeneral = themeManager.getComputedCssForCard("deck_1", "card_normal", category = "media")
        assertTrue(cssGeneral.contains("--polymath-accent: #ff007f;"))
        assertTrue(cssGeneral.contains("--polymath-surface: #112233;"))

        // Check card with card-level override takes precedence
        val cssSpecial = themeManager.getComputedCssForCard("deck_1", "card_special", category = "media")
        assertTrue(cssSpecial.contains("--polymath-accent: #00ffcc;"))
    }
}
