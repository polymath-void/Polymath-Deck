package com.polymathdeck

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.polymathdeck.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main application class for Polymath Deck.
 * Initializes Hilt dependency injection, notification channels,
 * and custom WorkManager configuration with HiltWorkerFactory.
 */
@HiltAndroidApp
class PolymathDeckApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Register system notification channels at app startup
        NotificationChannels.registerAll(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
