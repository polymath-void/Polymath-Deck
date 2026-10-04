package com.crescentdeck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crescentdeck.bridge.FeedUpdateState
import com.crescentdeck.data.db.entity.FeedEntity
import com.crescentdeck.data.db.entity.FeedItemEntity
import com.crescentdeck.data.repository.FeedRepository
import com.crescentdeck.engine.feed.OPMLManager
import com.crescentdeck.engine.feed.RSSFeedEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel managing RSS subscriptions, articles, manual refreshing, and OPML import/export.
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val feedEngine: RSSFeedEngine,
    val feedUpdateState: FeedUpdateState
) : ViewModel() {

    val feeds: StateFlow<List<FeedEntity>> = feedRepository.getAllFeedsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentArticles: StateFlow<List<FeedItemEntity>> = feedRepository.getRecentItemsFlow(100)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addFeed(url: String, title: String) {
        viewModelScope.launch {
            val feed = FeedEntity(
                feedId = UUID.randomUUID().toString(),
                url = url.trim(),
                title = title.ifBlank { "RSS Feed" }
            )
            feedRepository.upsertFeed(feed)
            feedEngine.refreshFeed(feed)
        }
    }

    fun refreshAll(deckId: String) {
        viewModelScope.launch {
            feedUpdateState.setRefreshing(true)
            val active = feedEngine.getActiveFeeds()
            for (feed in active) {
                val newItems = feedEngine.refreshFeed(feed)
                if (newItems.isNotEmpty()) {
                    feedEngine.materializeAsCards(deckId, feed, newItems)
                }
            }
            feedUpdateState.setRefreshing(false)
        }
    }

    fun importOPML(stream: InputStream) {
        viewModelScope.launch {
            val imported = OPMLManager.importOPML(stream)
            for (feed in imported) {
                feedRepository.upsertFeed(feed)
            }
        }
    }

    fun exportOPML(stream: OutputStream) {
        viewModelScope.launch {
            val all = feedEngine.getActiveFeeds()
            OPMLManager.exportOPML(all, stream)
        }
    }

    fun markRead(itemId: String) {
        viewModelScope.launch {
            feedRepository.markItemRead(itemId, true)
        }
    }

    fun deleteFeed(feedId: String) {
        viewModelScope.launch {
            feedRepository.deleteFeed(feedId)
        }
    }
}
