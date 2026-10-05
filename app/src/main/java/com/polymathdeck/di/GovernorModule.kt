package com.polymathdeck.di

import com.polymathdeck.engine.governor.DiskSnapshotManager
import com.polymathdeck.engine.governor.MemoryPressureMonitor
import com.polymathdeck.engine.governor.WebViewResourceGovernor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GovernorModule {

    @Provides
    @Singleton
    fun provideMemoryPressureMonitor(): MemoryPressureMonitor {
        return MemoryPressureMonitor()
    }

    @Provides
    @Singleton
    fun provideWebViewResourceGovernor(
        monitor: MemoryPressureMonitor,
        diskSnapshotManager: DiskSnapshotManager
    ): WebViewResourceGovernor {
        return WebViewResourceGovernor(monitor, diskSnapshotManager)
    }
}
