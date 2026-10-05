package com.polymathdeck.di

import com.polymathdeck.engine.physics.SpringDynamicsProcessor
import com.polymathdeck.engine.quadtree.QuadTreePhysicsEngine
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
