package com.polymathdeck.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an individual article or item fetched from an RSS feed.
 */
@Entity(
    tableName = "feed_items",
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["feedId"],
            childColumns = ["feedId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["cardId"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["feedId"]),
        Index(value = ["cardId"]),
        Index(value = ["guid"], unique = true),
        Index(value = ["publishedAt"])
    ]
)
data class FeedItemEntity(
    @PrimaryKey
    val itemId: String,
    val feedId: String,
    val cardId: String? = null,
    val guid: String,
    val title: String,
    val author: String = "",
    val contentHtml: String = "",
    val extractedText: String = "",
    val thumbnailUrl: String = "",
    val link: String = "",
    val publishedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isStarred: Boolean = false
)
