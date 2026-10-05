package com.polymathdeck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.polymathdeck.data.db.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for workspace decks.
 */
@Dao
interface DeckDao {

    @Query("SELECT * FROM decks ORDER BY sortOrder ASC, updatedAt DESC")
    fun getAllDecksFlow(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE deckId = :deckId LIMIT 1")
    suspend fun getDeckById(deckId: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE deckId = :deckId LIMIT 1")
    fun getDeckByIdFlow(deckId: String): Flow<DeckEntity?>

    @Upsert
    suspend fun upsertDeck(deck: DeckEntity)

    @Upsert
    suspend fun upsertDecks(decks: List<DeckEntity>)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE deckId = :deckId")
    suspend fun deleteDeckById(deckId: String)

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun getDeckCount(): Int
}
