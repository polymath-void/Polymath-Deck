package com.crescentdeck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.crescentdeck.data.db.entity.CardEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for 2D spatial card nodes.
 */
@Dao
interface CardDao {

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY priority DESC, lastActiveAt DESC")
    fun getCardsForDeckFlow(deckId: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId")
    suspend fun getCardsForDeck(deckId: String): List<CardEntity>

    @Query("""
        SELECT * FROM cards 
        WHERE deckId = :deckId 
        AND anchorX + width >= :minX 
        AND anchorX <= :maxX 
        AND anchorY + height >= :minY 
        AND anchorY <= :maxY
        ORDER BY priority DESC
    """)
    fun getCardsInBoundsFlow(
        deckId: String,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float
    ): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE cardId = :cardId LIMIT 1")
    suspend fun getCardById(cardId: String): CardEntity?

    @Query("SELECT * FROM cards WHERE cardId = :cardId LIMIT 1")
    fun getCardByIdFlow(cardId: String): Flow<CardEntity?>

    @Upsert
    suspend fun upsertCard(card: CardEntity)

    @Upsert
    suspend fun upsertCards(cards: List<CardEntity>)

    @Query("UPDATE cards SET anchorX = :x, anchorY = :y, lastActiveAt = :timestamp WHERE cardId = :cardId")
    suspend fun updatePosition(cardId: String, x: Float, y: Float, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE cards SET isHibernated = :isHibernated WHERE cardId = :cardId")
    suspend fun updateHibernation(cardId: String, isHibernated: Boolean)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE cardId = :cardId")
    suspend fun deleteCardById(cardId: String)

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteCardsForDeck(deckId: String)
}
