package com.crescentdeck.engine.feed

import com.crescentdeck.data.db.entity.FeedEntity
import com.crescentdeck.data.db.entity.FeedItemEntity
import com.crescentdeck.data.repository.DeckRepository
import com.crescentdeck.data.repository.FeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

data class NewArticlesEvent(
    val feed: FeedEntity,
    val items: List<FeedItemEntity>
)

/**
 * RSS Feed Engine responsible for network polling, conditional HTTP requests,
 * deduplication, and card materialization on the active deck canvas.
 */
@Singleton
class RSSFeedEngine @Inject constructor(
    private val httpClient: OkHttpClient,
    private val feedRepository: FeedRepository,
    private val deckRepository: DeckRepository
) {

    private val _newArticlesFlow = MutableSharedFlow<NewArticlesEvent>(extraBufferCapacity = 64)
    val newArticlesFlow: SharedFlow<NewArticlesEvent> = _newArticlesFlow.asSharedFlow()

    suspend fun getActiveFeeds(): List<FeedEntity> = feedRepository.getActiveFeeds()

    /**
     * Refreshes a feed using conditional GET (ETag / If-Modified-Since).
     * If 304 Not Modified is returned, skips parsing.
     */
    suspend fun refreshFeed(
        feed: FeedEntity,
        etag: String? = feed.etag,
        lastModified: String? = feed.lastModified
    ): List<FeedItemEntity> = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url(feed.url)
        if (!etag.isNullOrBlank()) {
            requestBuilder.header("If-None-Match", etag)
        }
        if (!lastModified.isNullOrBlank()) {
            requestBuilder.header("If-Modified-Since", lastModified)
        }

        val request = requestBuilder.build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.code == 304) {
                    // Not modified, no new items
                    return@withContext emptyList()
                }

                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }

                val bodyStream: InputStream = response.body?.byteStream() ?: return@withContext emptyList()
                val parsed = FeedParser.parse(bodyStream, feed.feedId)

                val newEtag = response.header("ETag")
                val newLastModified = response.header("Last-Modified")
                feedRepository.updateFeedRefreshMeta(
                    feedId = feed.feedId,
                    timestamp = System.currentTimeMillis(),
                    etag = newEtag,
                    lastModified = newLastModified
                )

                // Deduplicate against stored items
                val existingGuids = feedRepository.getExistingGuidsForFeed(feed.feedId).toSet()
                val freshItems = DeduplicationFilter.filterNewItems(parsed.items, existingGuids)

                if (freshItems.isNotEmpty()) {
                    feedRepository.upsertItems(freshItems)
                    _newArticlesFlow.tryEmit(NewArticlesEvent(feed, freshItems))
                }

                return@withContext freshItems
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    /**
     * Converts a list of new articles into spatial CardEntity instances on the specified [deckId].
     */
    suspend fun materializeAsCards(deckId: String, feed: FeedEntity, items: List<FeedItemEntity>) {
        val existingCards = deckRepository.getCardsForDeck(deckId)
        val startIndex = existingCards.size

        val cardsToInsert = items.mapIndexed { index, item ->
            val card = FeedCardFactory.createCardForArticle(deckId, item, startIndex + index)
            feedRepository.associateCard(item.itemId, card.cardId)
            card
        }

        deckRepository.upsertCards(cardsToInsert)
    }
}
