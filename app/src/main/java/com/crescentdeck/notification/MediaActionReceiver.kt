package com.crescentdeck.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import com.crescentdeck.engine.media.MediaPlaybackEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Handles notification and PiP action button broadcasts (Play/Pause, Skip, Close).
 */
@UnstableApi
@AndroidEntryPoint
class MediaActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var playbackEngine: MediaPlaybackEngine

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.crescentdeck.ACTION_PLAY_PAUSE" -> {
                val player = playbackEngine.getActivePlayer()
                if (player != null) {
                    if (player.isPlaying) {
                        playbackEngine.pause()
                    } else {
                        playbackEngine.resume()
                    }
                }
            }
            "com.crescentdeck.ACTION_SKIP_NEXT" -> {
                playbackEngine.getActivePlayer()?.seekForward()
            }
            "com.crescentdeck.ACTION_SKIP_PREV" -> {
                playbackEngine.getActivePlayer()?.seekBack()
            }
            "com.crescentdeck.ACTION_CLOSE" -> {
                val activeCardId = playbackEngine.playbackState.value.activeCardId
                if (activeCardId != null) {
                    playbackEngine.stopAndRelease(activeCardId)
                } else {
                    playbackEngine.pause()
                }
            }
        }
    }
}
