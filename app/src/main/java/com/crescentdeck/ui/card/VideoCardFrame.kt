package com.crescentdeck.ui.card

import android.view.TextureView
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import com.crescentdeck.R
import com.crescentdeck.data.db.entity.CardEntity
import com.crescentdeck.engine.media.MediaPlaybackEngine

/**
 * Video card frame rendering hardware-accelerated video playback
 * with inline transport controls, fullscreen expansion, and PiP dispatch.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoCardFrame(
    card: CardEntity,
    mediaPlaybackEngine: MediaPlaybackEngine,
    onFullscreenRequested: (cardId: String) -> Unit,
    onPiPRequested: (cardId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by mediaPlaybackEngine.playbackState.collectAsState()
    val isThisCardPlaying = playbackState.activeCardId == card.cardId && playbackState.isPlaying

    val mediaUri = remember(card.contentPayload) {
        if (card.contentPayload.startsWith("http")) {
            card.contentPayload
        } else {
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        }
    }

    DisposableEffect(card.cardId) {
        onDispose {
            if (playbackState.activeCardId == card.cardId) {
                mediaPlaybackEngine.stopAndRelease(card.cardId)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            val player = mediaPlaybackEngine.playMedia(card.cardId, mediaUri, "Video #${card.cardId.take(4)}")
                            player.setVideoTextureView(this@apply)
                        }

                        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                            mediaPlaybackEngine.getActivePlayer()?.clearVideoTextureView(this@apply)
                            return true
                        }
                        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                    }
                }
            }
        )

        // Overlay transport bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF38BDF8), CircleShape)
                    .clickable {
                        if (isThisCardPlaying) {
                            mediaPlaybackEngine.pause()
                        } else {
                            mediaPlaybackEngine.resume()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isThisCardPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = " Video Card",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )

            // PiP Button
            Text(
                text = "PiP",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { onPiPRequested(card.cardId) }
                    .padding(horizontal = 8.dp)
            )

            // Fullscreen Button
            Text(
                text = "FULL",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { onFullscreenRequested(card.cardId) }
                    .padding(horizontal = 8.dp)
            )
        }
    }
}
