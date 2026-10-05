package com.crescentdeck.theme

import com.crescentdeck.ui.theme.CrescentTheme
import com.crescentdeck.ui.theme.ThemeManager
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
        themeManager.setGlobalTheme(CrescentTheme.DefaultDark)
        val css = themeManager.getComputedCssForCard("deck_1", "card_1")

        assertTrue(css.contains("--crescent-bg:"))
        assertTrue(css.contains("--crescent-accent:"))
    }

    @Test
    fun testCategoryAndCardCascadeOverrides() {
        themeManager.setGlobalTheme(CrescentTheme.DefaultDark)

        // Category override
        themeManager.setCategoryOverride("media", mapOf("--crescent-accent" to "#ff007f"))

        // Deck override
        themeManager.setDeckOverride("deck_1", mapOf("--crescent-surface" to "#112233"))

        // Card override
        themeManager.setCardOverride("card_special", mapOf("--crescent-accent" to "#00ffcc"))

        // Check card without card-level override gets category and deck overrides
        val cssGeneral = themeManager.getComputedCssForCard("deck_1", "card_normal", category = "media")
        assertTrue(cssGeneral.contains("--crescent-accent: #ff007f;"))
        assertTrue(cssGeneral.contains("--crescent-surface: #112233;"))

        // Check card with card-level override takes precedence
        val cssSpecial = themeManager.getComputedCssForCard("deck_1", "card_special", category = "media")
        assertTrue(cssSpecial.contains("--crescent-accent: #00ffcc;"))
    }
}
