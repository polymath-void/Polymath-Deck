package com.crescentdeck.di

import android.content.Context
import androidx.room.Room
import com.crescentdeck.data.db.CrescentDeckDatabase
import com.crescentdeck.data.db.dao.AdapterDao
import com.crescentdeck.data.db.dao.CardDao
import com.crescentdeck.data.db.dao.CrashCheckpointDao
import com.crescentdeck.data.db.dao.DeckDao
import com.crescentdeck.data.db.dao.FeedDao
import com.crescentdeck.data.db.dao.FeedItemDao
import com.crescentdeck.data.db.dao.MediaSessionDao
import com.crescentdeck.data.db.dao.TabDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CrescentDeckDatabase {
        return Room.databaseBuilder(
            context,
            CrescentDeckDatabase::class.java,
            CrescentDeckDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDeckDao(database: CrescentDeckDatabase): DeckDao = database.deckDao()

    @Provides
    fun provideCardDao(database: CrescentDeckDatabase): CardDao = database.cardDao()

    @Provides
    fun provideAdapterDao(database: CrescentDeckDatabase): AdapterDao = database.adapterDao()

    @Provides
    fun provideTabDao(database: CrescentDeckDatabase): TabDao = database.tabDao()

    @Provides
    fun provideFeedDao(database: CrescentDeckDatabase): FeedDao = database.feedDao()

    @Provides
    fun provideFeedItemDao(database: CrescentDeckDatabase): FeedItemDao = database.feedItemDao()

    @Provides
    fun provideMediaSessionDao(database: CrescentDeckDatabase): MediaSessionDao = database.mediaSessionDao()

    @Provides
    fun provideCrashCheckpointDao(database: CrescentDeckDatabase): CrashCheckpointDao = database.crashCheckpointDao()

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
