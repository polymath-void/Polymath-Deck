package com.polymathdeck.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design system theme specification for Polymath Deck.
 */
data class PolymathTheme(
    val id: String = "dark_slate",
    val name: String = "Dark Slate",
    val background: Color = Color(0xFF0F172A),
    val surface: Color = Color(0xFF1E293B),
    val surfaceVariant: Color = Color(0xFF334155),
    val accentPrimary: Color = Color(0xFF38BDF8),
    val accentSecondary: Color = Color(0xFF818CF8),
    val textPrimary: Color = Color(0xFFF8FAFC),
    val textSecondary: Color = Color(0xFF94A3B8),
    val cardCornerRadiusDp: Float = 16f,
    val cardElevationDp: Float = 6f
) {
    /**
     * Converts theme colors into CSS custom property definitions
     * for injection into WebView LiveCardFrame instances.
     */
    fun toCssVariables(): String {
        return """
            :root {
                --polymath-bg: #${(background.value shr 32).toString(16).padStart(6, '0')};
                --polymath-surface: #${(surface.value shr 32).toString(16).padStart(6, '0')};
                --polymath-accent: #${(accentPrimary.value shr 32).toString(16).padStart(6, '0')};
                --polymath-text: #${(textPrimary.value shr 32).toString(16).padStart(6, '0')};
                --polymath-text-muted: #${(textSecondary.value shr 32).toString(16).padStart(6, '0')};
                --polymath-radius: ${cardCornerRadiusDp}px;
            }
        """.trimIndent()
    }

    companion object {
        val DefaultDark = PolymathTheme()

        val EmeraldOasis = PolymathTheme(
            id = "emerald_oasis",
            name = "Emerald Oasis",
            background = Color(0xFF064E3B),
            surface = Color(0xFF065F46),
            accentPrimary = Color(0xFF34D399),
            textPrimary = Color(0xFFECFDF5)
        )

        val CyberpunkRose = PolymathTheme(
            id = "cyberpunk_rose",
            name = "Cyberpunk Rose",
            background = Color(0xFF180A1F),
            surface = Color(0xFF2C1038),
            accentPrimary = Color(0xFFFB7185),
            textPrimary = Color(0xFFFFF1F2)
        )
    }
}
