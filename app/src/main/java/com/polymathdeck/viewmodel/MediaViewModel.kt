package com.polymathdeck.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import com.polymathdeck.engine.media.MediaPlaybackEngine
import com.polymathdeck.pip.FloatingPiPController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel managing media playback states, PiP overlay transitions,
 * and track parameters.
 */
@OptIn(UnstableApi::class)
@HiltViewModel
class MediaViewModel @Inject constructor(
    val mediaPlaybackEngine: MediaPlaybackEngine,
    val pipController: FloatingPiPController
) : ViewModel() {

    val playbackState = mediaPlaybackEngine.playbackState

    fun enterPiP(activity: Activity) {
        val state = playbackState.value
        pipController.enterPiP(activity, isPlaying = state.isPlaying)
    }

    fun play() = mediaPlaybackEngine.resume()
    fun pause() = mediaPlaybackEngine.pause()
    fun seekTo(posMs: Long) = mediaPlaybackEngine.seekTo(posMs)
    fun setSpeed(speed: Float) = mediaPlaybackEngine.setSpeed(speed)
}
