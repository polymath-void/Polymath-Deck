package com.crescentdeck.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an RSS / Atom / JSON feed subscription.
 */
@Entity(tableName = "feeds")
data class FeedEntity(
    @PrimaryKey
    val feedId: String,
    val url: String,
    val title: String,
    val siteUrl: String = "",
    val faviconUrl: String = "",
    val refreshIntervalMinutes: Int = 30,
    val lastRefreshedAt: Long = 0L,
    val etag: String? = null,
    val lastModified: String? = null,
    val isActive: Boolean = true
)
