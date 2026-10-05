package com.polymathdeck.bridge

import com.polymathdeck.engine.media.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reactive bridge for media playback state updates across UI and foreground services.
 */
@Singleton
class MediaSessionState @Inject constructor() {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun updatePlaybackState(playbackState: PlaybackState) {
        _state.value = playbackState
    }

    fun updatePosition(posMs: Long) {
        _state.value = _state.value.copy(currentPositionMs = posMs)
    }

    fun setPlaying(playing: Boolean) {
        _state.value = _state.value.copy(isPlaying = playing)
    }
}
