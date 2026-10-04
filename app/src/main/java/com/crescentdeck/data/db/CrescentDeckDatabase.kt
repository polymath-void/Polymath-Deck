package com.crescentdeck.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.crescentdeck.data.db.dao.AdapterDao
import com.crescentdeck.data.db.dao.CardDao
import com.crescentdeck.data.db.dao.CrashCheckpointDao
import com.crescentdeck.data.db.dao.DeckDao
import com.crescentdeck.data.db.dao.FeedDao
import com.crescentdeck.data.db.dao.FeedItemDao
import com.crescentdeck.data.db.dao.MediaSessionDao
import com.crescentdeck.data.db.dao.TabDao
import com.crescentdeck.data.db.entity.AdapterEntity
import com.crescentdeck.data.db.entity.CardEntity
import com.crescentdeck.data.db.entity.CrashCheckpointEntity
import com.crescentdeck.data.db.entity.DeckEntity
import com.crescentdeck.data.db.entity.FeedEntity
import com.crescentdeck.data.db.entity.FeedItemEntity
import com.crescentdeck.data.db.entity.MediaSessionEntity
import com.crescentdeck.data.db.entity.TabEntity
import com.crescentdeck.data.db.entity.TabGroupEntity

/**
 * Main Room Database for the Crescent Deck platform.
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
abstract class CrescentDeckDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun adapterDao(): AdapterDao
    abstract fun tabDao(): TabDao
    abstract fun feedDao(): FeedDao
    abstract fun feedItemDao(): FeedItemDao
    abstract fun mediaSessionDao(): MediaSessionDao
    abstract fun crashCheckpointDao(): CrashCheckpointDao

    companion object {
        const val DATABASE_NAME = "crescent_deck.db"
    }
}
