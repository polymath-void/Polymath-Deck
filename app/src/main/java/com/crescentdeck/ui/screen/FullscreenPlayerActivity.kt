package com.crescentdeck.ui.screen

import android.content.res.Configuration
import android.os.Bundle
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.crescentdeck.engine.media.MediaPlaybackEngine
import com.crescentdeck.pip.FloatingPiPController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Dedicated Activity providing immersive, hardware-accelerated fullscreen video playback
 * and seamless handoff into Android Picture-in-Picture (PiP).
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class FullscreenPlayerActivity : ComponentActivity() {

    @Inject
    lateinit var mediaPlaybackEngine: MediaPlaybackEngine

    @Inject
    lateinit var pipController: FloatingPiPController

    private var surfaceView: SurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frameLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        surfaceView = SurfaceView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            holder.addCallback(object : android.view.SurfaceHolder.Callback {
                override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                    mediaPlaybackEngine.transitionToFullscreen(this@apply, null)
                }

                override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}

                override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                    mediaPlaybackEngine.getActivePlayer()?.clearVideoSurfaceView(this@apply)
                }
            })
        }

        frameLayout.addView(surfaceView)
        setContentView(frameLayout)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Automatically enter PiP when home button is pressed
        pipController.enterPiP(this, isPlaying = mediaPlaybackEngine.playbackState.value.isPlaying)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            pipController.onExitPiP()
        }
    }
}
