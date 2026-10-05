package com.polymathdeck.data.repository

import com.polymathdeck.data.db.dao.AdapterDao
import com.polymathdeck.data.db.dao.CardDao
import com.polymathdeck.data.db.dao.DeckDao
import com.polymathdeck.data.db.entity.AdapterEntity
import com.polymathdeck.data.db.entity.CardEntity
import com.polymathdeck.data.db.entity.DeckEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository providing single source of truth for Decks, 2D Spatial Cards, and Adapters.
 */
@Singleton
class DeckRepository @Inject constructor(
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
    private val adapterDao: AdapterDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getAllDecksFlow(): Flow<List<DeckEntity>> = deckDao.getAllDecksFlow()

    fun getDeckByIdFlow(deckId: String): Flow<DeckEntity?> = deckDao.getDeckByIdFlow(deckId)

    suspend fun getDeckById(deckId: String): DeckEntity? = withContext(ioDispatcher) {
        deckDao.getDeckById(deckId)
    }

    suspend fun upsertDeck(deck: DeckEntity) = withContext(ioDispatcher) {
        deckDao.upsertDeck(deck)
    }

    suspend fun deleteDeck(deckId: String) = withContext(ioDispatcher) {
        deckDao.deleteDeckById(deckId)
    }

    fun getCardsForDeckFlow(deckId: String): Flow<List<CardEntity>> =
        cardDao.getCardsForDeckFlow(deckId)

    suspend fun getCardsForDeck(deckId: String): List<CardEntity> = withContext(ioDispatcher) {
        cardDao.getCardsForDeck(deckId)
    }

    fun getCardsInBoundsFlow(
        deckId: String,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float
    ): Flow<List<CardEntity>> = cardDao.getCardsInBoundsFlow(deckId, minX, minY, maxX, maxY)

    suspend fun getCardById(cardId: String): CardEntity? = withContext(ioDispatcher) {
        cardDao.getCardById(cardId)
    }

    suspend fun upsertCard(card: CardEntity) = withContext(ioDispatcher) {
        cardDao.upsertCard(card)
    }

    suspend fun upsertCards(cards: List<CardEntity>) = withContext(ioDispatcher) {
        cardDao.upsertCards(cards)
    }

    suspend fun updateCardPosition(cardId: String, x: Float, y: Float) = withContext(ioDispatcher) {
        cardDao.updatePosition(cardId, x, y)
    }

    suspend fun updateCardHibernation(cardId: String, isHibernated: Boolean) = withContext(ioDispatcher) {
        cardDao.updateHibernation(cardId, isHibernated)
    }

    suspend fun deleteCard(cardId: String) = withContext(ioDispatcher) {
        cardDao.deleteCardById(cardId)
    }

    suspend fun deleteCardsForDeck(deckId: String) = withContext(ioDispatcher) {
        cardDao.deleteCardsForDeck(deckId)
    }

    fun getAdapterForCardFlow(cardId: String): Flow<AdapterEntity?> =
        adapterDao.getAdapterForCardFlow(cardId)

    suspend fun getAdapterForCard(cardId: String): AdapterEntity? = withContext(ioDispatcher) {
        adapterDao.getAdapterForCard(cardId)
    }

    suspend fun upsertAdapter(adapter: AdapterEntity) = withContext(ioDispatcher) {
        adapterDao.upsertAdapter(adapter)
    }
}
