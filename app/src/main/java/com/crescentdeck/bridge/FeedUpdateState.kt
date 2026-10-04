package com.crescentdeck.bridge

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class FeedRefreshEvent(
    val feedId: String,
    val newArticleCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Reactive bridge for RSS feed updates, unread counters, and background refresh events.
 */
@Singleton
class FeedUpdateState @Inject constructor() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshEvents = MutableSharedFlow<FeedRefreshEvent>(extraBufferCapacity = 32)
    val refreshEvents: SharedFlow<FeedRefreshEvent> = _refreshEvents.asSharedFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }

    fun emitRefreshEvent(event: FeedRefreshEvent) {
        _refreshEvents.tryEmit(event)
    }

    fun setUnreadCount(count: Int) {
        _unreadCount.value = count
    }
}
