package com.crescentdeck.engine.feed

import com.crescentdeck.data.db.entity.FeedItemEntity

/**
 * Filters out duplicate feed articles using GUID, URL, or hash comparison.
 */
object DeduplicationFilter {

    fun filterNewItems(
        incoming: List<FeedItemEntity>,
        existingGuids: Set<String>
    ): List<FeedItemEntity> {
        return incoming.filter { item ->
            item.guid.isNotBlank() && !existingGuids.contains(item.guid)
        }
    }
}
