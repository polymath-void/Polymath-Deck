package com.crescentdeck.engine.media

import android.content.Context
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configures adaptive bitrate and track selection parameters for HLS/DASH playback.
 */
@Singleton
class AdaptiveTrackSelector @Inject constructor() {

    fun createTrackSelector(context: Context): DefaultTrackSelector {
        val parameters = DefaultTrackSelector.Parameters.Builder(context)
            .setMaxVideoSizeSd()
            .setForceLowestBitrate(false)
            .setAllowVideoMixedMimeTypeAdaptiveness(true)
            .setAllowAudioMixedMimeTypeAdaptiveness(true)
            .build()

        return DefaultTrackSelector(context, parameters)
    }
}
