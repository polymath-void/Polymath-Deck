package com.crescentdeck.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Adapter configuration entity for custom CSS, JS scripts, and custom layout rules per card.
 */
@Entity(
    tableName = "adapters",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["cardId"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cardId"])
    ]
)
data class AdapterEntity(
    @PrimaryKey
    val adapterId: String,
    val cardId: String,
    val adapterType: String,
    val configJson: String = "{}",
    val cssInjectionRules: String = ""
)
