package com.crescentdeck.ui.intent

import com.crescentdeck.data.db.entity.CardEntity
import com.crescentdeck.data.db.entity.DeckEntity

/**
 * Immutable MVI ViewState representing the active viewport and spatial deck.
 */
data class DeckViewState(
    val currentDeck: DeckEntity? = null,
    val cards: List<CardEntity> = emptyList(),
    val viewportPanX: Float = 0f,
    val viewportPanY: Float = 0f,
    val viewportScale: Float = 1.0f,
    val focusedCardId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
