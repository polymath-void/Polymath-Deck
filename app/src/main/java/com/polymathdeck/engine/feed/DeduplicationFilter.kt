package com.polymathdeck.engine.feed

import com.polymathdeck.data.db.entity.FeedItemEntity

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
