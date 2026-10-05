package com.polymathdeck.di

import androidx.media3.common.util.UnstableApi
import com.polymathdeck.data.repository.MediaRepository
import com.polymathdeck.engine.governor.MemoryPressureMonitor
import com.polymathdeck.engine.media.AdaptiveTrackSelector
import com.polymathdeck.engine.media.CodecSelector
import com.polymathdeck.engine.media.ExoPlayerPool
import com.polymathdeck.engine.media.MediaPlaybackEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideAdaptiveTrackSelector(): AdaptiveTrackSelector {
        return AdaptiveTrackSelector()
    }

    @Provides
    @Singleton
    fun provideCodecSelector(): CodecSelector {
        return CodecSelector()
    }

    @Provides
    @Singleton
    fun provideMediaPlaybackEngine(
        playerPool: ExoPlayerPool,
        mediaRepository: MediaRepository,
        memoryMonitor: MemoryPressureMonitor
    ): MediaPlaybackEngine {
        return MediaPlaybackEngine(playerPool, mediaRepository, memoryMonitor)
    }
}
