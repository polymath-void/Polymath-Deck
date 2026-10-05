package com.polymathdeck.ui.screen

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.polymathdeck.notification.DeepLinkRouter
import com.polymathdeck.service.BackgroundPlaybackService
import com.polymathdeck.ui.theme.ThemeManager
import com.polymathdeck.viewmodel.DeckViewModel
import com.polymathdeck.viewmodel.FeedViewModel
import com.polymathdeck.viewmodel.MediaViewModel
import com.polymathdeck.viewmodel.TabViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Root Activity initializing Hilt ViewModels, deep link routing,
 * PiP gestures, and Compose UI hosting.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val deckViewModel: DeckViewModel by viewModels()
    private val tabViewModel: TabViewModel by viewModels()
    private val feedViewModel: FeedViewModel by viewModels()
    private val mediaViewModel: MediaViewModel by viewModels()

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parse deep link if opened via notification or external intent
        handleDeepLink(intent)

        setContent {
            DeckCanvasScreen(
                deckViewModel = deckViewModel,
                tabViewModel = tabViewModel,
                feedViewModel = feedViewModel,
                mediaViewModel = mediaViewModel,
                themeManager = themeManager,
                onLaunchFullscreen = { cardId ->
                    val intent = Intent(this, FullscreenPlayerActivity::class.java).apply {
                        putExtra("cardId", cardId)
                    }
                    startActivity(intent)
                },
                onOpenUrlExternal = { url ->
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(browserIntent)
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val cardId = DeepLinkRouter.parseCardId(data)
        if (cardId != null) {
            val node = deckViewModel.quadTreeEngine.locateNode(cardId)
            if (node != null) {
                // Focus card
                node.isSleeping = false
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If media playback is active, start foreground service and enter PiP
        if (mediaViewModel.playbackState.value.isPlaying) {
            val serviceIntent = Intent(this, BackgroundPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            mediaViewModel.enterPiP(this)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            mediaViewModel.pipController.onExitPiP()
        }
    }
}
