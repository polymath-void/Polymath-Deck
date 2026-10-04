package com.crescentdeck.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a browser/deck tab.
 */
@Entity(
    tableName = "tabs",
    foreignKeys = [
        ForeignKey(
            entity = TabGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["deckId"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["deckId"]),
        Index(value = ["sortOrder"])
    ]
)
data class TabEntity(
    @PrimaryKey
    val tabId: String,
    val groupId: String? = null,
    val deckId: String,
    val title: String,
    val faviconUri: String? = null,
    val isActive: Boolean = false,
    val isHibernated: Boolean = false,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)
