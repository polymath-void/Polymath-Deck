package com.polymathdeck.engine.feed

import com.polymathdeck.data.db.entity.CardEntity
import com.polymathdeck.data.db.entity.CardType
import com.polymathdeck.data.db.entity.FeedItemEntity
import org.json.JSONObject
import java.util.UUID

/**
 * Transforms fetched RSS articles into interactive spatial CardEntities.
 */
object FeedCardFactory {

    fun createCardForArticle(
        deckId: String,
        item: FeedItemEntity,
        gridIndex: Int,
        columns: Int = 3,
        cardWidth: Float = 320f,
        cardHeight: Float = 260f,
        padding: Float = 24f
    ): CardEntity {
        val col = gridIndex % columns
        val row = gridIndex / columns

        val posX = 50f + col * (cardWidth + padding)
        val posY = 100f + row * (cardHeight + padding)

        val payload = JSONObject().apply {
            put("title", item.title)
            put("author", item.author)
            put("summary", item.extractedText.take(280))
            put("link", item.link)
            put("thumbnail", item.thumbnailUrl)
            put("publishedAt", item.publishedAt)
            put("feedId", item.feedId)
        }.toString()

        val cardId = UUID.randomUUID().toString()

        return CardEntity(
            cardId = cardId,
            deckId = deckId,
            cardType = CardType.ARTICLE.typeId,
            anchorX = posX,
            anchorY = posY,
            width = cardWidth,
            height = cardHeight,
            contentPayload = payload,
            priority = 2,
            isHibernated = false,
            lastActiveAt = item.publishedAt
        )
    }
}
