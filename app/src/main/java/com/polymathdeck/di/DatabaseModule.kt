package com.polymathdeck.di

import android.content.Context
import androidx.room.Room
import com.polymathdeck.data.db.PolymathDeckDatabase
import com.polymathdeck.data.db.dao.AdapterDao
import com.polymathdeck.data.db.dao.CardDao
import com.polymathdeck.data.db.dao.CrashCheckpointDao
import com.polymathdeck.data.db.dao.DeckDao
import com.polymathdeck.data.db.dao.FeedDao
import com.polymathdeck.data.db.dao.FeedItemDao
import com.polymathdeck.data.db.dao.MediaSessionDao
import com.polymathdeck.data.db.dao.TabDao
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
    fun provideDatabase(@ApplicationContext context: Context): PolymathDeckDatabase {
        return Room.databaseBuilder(
            context,
            PolymathDeckDatabase::class.java,
            PolymathDeckDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDeckDao(database: PolymathDeckDatabase): DeckDao = database.deckDao()

    @Provides
    fun provideCardDao(database: PolymathDeckDatabase): CardDao = database.cardDao()

    @Provides
    fun provideAdapterDao(database: PolymathDeckDatabase): AdapterDao = database.adapterDao()

    @Provides
    fun provideTabDao(database: PolymathDeckDatabase): TabDao = database.tabDao()

    @Provides
    fun provideFeedDao(database: PolymathDeckDatabase): FeedDao = database.feedDao()

    @Provides
    fun provideFeedItemDao(database: PolymathDeckDatabase): FeedItemDao = database.feedItemDao()

    @Provides
    fun provideMediaSessionDao(database: PolymathDeckDatabase): MediaSessionDao = database.mediaSessionDao()

    @Provides
    fun provideCrashCheckpointDao(database: PolymathDeckDatabase): CrashCheckpointDao = database.crashCheckpointDao()

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
