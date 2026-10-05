package com.polymathdeck.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.polymathdeck.data.db.dao.AdapterDao
import com.polymathdeck.data.db.dao.CardDao
import com.polymathdeck.data.db.dao.CrashCheckpointDao
import com.polymathdeck.data.db.dao.DeckDao
import com.polymathdeck.data.db.dao.FeedDao
import com.polymathdeck.data.db.dao.FeedItemDao
import com.polymathdeck.data.db.dao.MediaSessionDao
import com.polymathdeck.data.db.dao.TabDao
import com.polymathdeck.data.db.entity.AdapterEntity
import com.polymathdeck.data.db.entity.CardEntity
import com.polymathdeck.data.db.entity.CrashCheckpointEntity
import com.polymathdeck.data.db.entity.DeckEntity
import com.polymathdeck.data.db.entity.FeedEntity
import com.polymathdeck.data.db.entity.FeedItemEntity
import com.polymathdeck.data.db.entity.MediaSessionEntity
import com.polymathdeck.data.db.entity.TabEntity
import com.polymathdeck.data.db.entity.TabGroupEntity

/**
 * Main Room Database for the Polymath Deck platform.
 */
@Database(
    entities = [
        DeckEntity::class,
        CardEntity::class,
        AdapterEntity::class,
        TabGroupEntity::class,
        TabEntity::class,
        FeedEntity::class,
        FeedItemEntity::class,
        MediaSessionEntity::class,
        CrashCheckpointEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PolymathDeckDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun adapterDao(): AdapterDao
    abstract fun tabDao(): TabDao
    abstract fun feedDao(): FeedDao
    abstract fun feedItemDao(): FeedItemDao
    abstract fun mediaSessionDao(): MediaSessionDao
    abstract fun crashCheckpointDao(): CrashCheckpointDao

    companion object {
        const val DATABASE_NAME = "polymath_deck.db"
    }
}
