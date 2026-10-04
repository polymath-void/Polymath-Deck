package com.crescentdeck.di

import com.crescentdeck.engine.physics.SpringDynamicsProcessor
import com.crescentdeck.engine.quadtree.QuadTreePhysicsEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideQuadTreePhysicsEngine(): QuadTreePhysicsEngine {
        return QuadTreePhysicsEngine()
    }

    @Provides
    @Singleton
    fun provideSpringDynamicsProcessor(engine: QuadTreePhysicsEngine): SpringDynamicsProcessor {
        return SpringDynamicsProcessor(engine)
    }
}
