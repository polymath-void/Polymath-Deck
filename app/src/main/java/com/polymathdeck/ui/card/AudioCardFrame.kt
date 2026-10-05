package com.polymathdeck.ui.card

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.polymathdeck.R
import com.polymathdeck.data.db.entity.CardEntity
import com.polymathdeck.engine.media.MediaPlaybackEngine

/**
 * Audio player card frame with playback controls and track progress bar.
 */
@OptIn(UnstableApi::class)
@Composable
fun AudioCardFrame(
    card: CardEntity,
    mediaPlaybackEngine: MediaPlaybackEngine,
    modifier: Modifier = Modifier
) {
    val playbackState by mediaPlaybackEngine.playbackState.collectAsState()
    val isPlaying = playbackState.activeCardId == card.cardId && playbackState.isPlaying

    val audioUri = remember(card.contentPayload) {
        if (card.contentPayload.startsWith("http")) card.contentPayload else "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF818CF8), CircleShape)
                        .clickable {
                            if (isPlaying) {
                                mediaPlaybackEngine.pause()
                            } else {
                                mediaPlaybackEngine.playMedia(card.cardId, audioUri, "Audio #${card.cardId.take(4)}")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = "Audio Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "Audio Track #${card.cardId.take(4)}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isPlaying) "Playing in background..." else "Paused",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            LinearProgressIndicator(
                progress = {
                    if (playbackState.durationMs > 0) {
                        playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()
                    } else 0f
                },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF818CF8),
                trackColor = Color(0xFF334155)
            )
        }
    }
}
