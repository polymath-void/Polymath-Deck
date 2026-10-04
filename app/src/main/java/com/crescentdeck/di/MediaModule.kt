package com.crescentdeck.di

import androidx.media3.common.util.UnstableApi
import com.crescentdeck.data.repository.MediaRepository
import com.crescentdeck.engine.governor.MemoryPressureMonitor
import com.crescentdeck.engine.media.AdaptiveTrackSelector
import com.crescentdeck.engine.media.CodecSelector
import com.crescentdeck.engine.media.ExoPlayerPool
import com.crescentdeck.engine.media.MediaPlaybackEngine
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
