package com.polymathdeck.engine.media

import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.polymathdeck.data.repository.MediaRepository
import com.polymathdeck.engine.governor.MemoryPressureMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val activeCardId: String? = null,
    val mediaUri: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val title: String = ""
)

/**
 * Core media playback coordinator for video, audio, and streaming pipelines.
 */
@UnstableApi
@Singleton
class MediaPlaybackEngine @Inject constructor(
    private val playerPool: ExoPlayerPool,
    private val mediaRepository: MediaRepository,
    private val memoryMonitor: MemoryPressureMonitor
) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var activePlayer: ExoPlayer? = null

    /**
     * Prepares and starts playback for a given card with [mediaUri].
     */
    fun playMedia(cardId: String, mediaUri: String, title: String = "", startPosMs: Long = 0L): ExoPlayer {
        memoryMonitor.isMediaPlaybackActive = true

        val player = playerPool.acquirePlayer(cardId, PlayerSlot.PRIMARY)
        activePlayer = player

        val mediaItem = MediaItem.fromUri(Uri.parse(mediaUri))
        player.setMediaItem(mediaItem)
        if (startPosMs > 0L) {
            player.seekTo(startPosMs)
        }
        player.prepare()
        player.playWhenReady = true

        _playbackState.value = PlaybackState(
            activeCardId = cardId,
            mediaUri = mediaUri,
            isPlaying = true,
            title = title,
            currentPositionMs = startPosMs
        )

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = isPlaying,
                    currentPositionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(0L)
                )
                scope.launch {
                    mediaRepository.updatePlayingState(cardId, isPlaying)
                    mediaRepository.updatePosition(cardId, player.currentPosition)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                }
            }
        })

        return player
    }

    fun pause() {
        activePlayer?.pause()
    }

    fun resume() {
        activePlayer?.play()
    }

    fun seekTo(positionMs: Long) {
        activePlayer?.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        activePlayer?.playbackParameters = PlaybackParameters(speed)
        _playbackState.value = _playbackState.value.copy(speed = speed)
    }

    /**
     * Seamless surface transition: detach from inline TextureView and attach to Fullscreen SurfaceView
     * with zero audio/video interruption.
     */
    fun transitionToFullscreen(fullscreenSurface: SurfaceView, inlineTexture: TextureView?) {
        val player = activePlayer ?: return
        if (inlineTexture != null) {
            player.clearVideoTextureView(inlineTexture)
        }
        player.setVideoSurfaceView(fullscreenSurface)
    }

    /**
     * Seamless surface transition: attach back to inline TextureView when exiting fullscreen.
     */
    fun transitionToInline(inlineTexture: TextureView, fullscreenSurface: SurfaceView?) {
        val player = activePlayer ?: return
        if (fullscreenSurface != null) {
            player.clearVideoSurfaceView(fullscreenSurface)
        }
        player.setVideoTextureView(inlineTexture)
    }

    fun stopAndRelease(cardId: String) {
        if (_playbackState.value.activeCardId == cardId) {
            activePlayer?.stop()
            activePlayer = null
            _playbackState.value = PlaybackState()
            memoryMonitor.isMediaPlaybackActive = false
        }
        playerPool.releaseCard(cardId)
    }

    fun getActivePlayer(): ExoPlayer? = activePlayer
}
