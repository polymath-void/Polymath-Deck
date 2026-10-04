package com.crescentdeck.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a workspace deck or canvas containing cards.
 */
@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey
    val deckId: String,
    val title: String,
    val sortOrder: Int = 0,
    val themeOverrideJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
