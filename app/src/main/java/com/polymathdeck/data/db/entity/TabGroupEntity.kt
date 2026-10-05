package com.polymathdeck.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a group/category for tabs.
 */
@Entity(tableName = "tab_groups")
data class TabGroupEntity(
    @PrimaryKey
    val groupId: String,
    val label: String,
    val colorTag: Int = 0xFF38BDF8.toInt(),
    val sortOrder: Int = 0
)
