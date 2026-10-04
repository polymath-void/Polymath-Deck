package com.crescentdeck.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a card node positioned in 2D space on a deck.
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["deckId"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["deckId", "anchorX", "anchorY"])
    ]
)
data class CardEntity(
    @PrimaryKey
    val cardId: String,
    val deckId: String,
    val cardType: Int = CardType.NATIVE.typeId,
    val anchorX: Float = 0f,
    val anchorY: Float = 0f,
    val width: Float = 320f,
    val height: Float = 240f,
    val contentPayload: String = "",
    val priority: Int = 1,
    val isHibernated: Boolean = false,
    val lastActiveAt: Long = System.currentTimeMillis()
)
