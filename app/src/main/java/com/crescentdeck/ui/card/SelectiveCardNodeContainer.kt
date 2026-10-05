package com.crescentdeck.ui.card

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.crescentdeck.data.db.entity.CardEntity
import com.crescentdeck.data.db.entity.CardType
import com.crescentdeck.engine.governor.CardLifecycleState
import com.crescentdeck.engine.governor.WebViewResourceGovernor
import com.crescentdeck.engine.media.MediaPlaybackEngine
import com.crescentdeck.engine.physics.NodeDelta
import com.crescentdeck.ui.theme.ThemeManager

/**
 * SelectiveCardNodeContainer hosts individual cards on the GPU render layer.
 *
 * Implements strict Gesture Segregation:
 * - Header Bar (0-32dp): Window management gestures (drag, double-tap to immersive, minimize, close).
 *   Consumes touches completely, preventing event conflict with the underlying web content.
 * - Card Body (32dp+): Content view (WebView, Video, Reader). Internal scrolling and page interaction
 *   receive all touch events unintercepted.
 */
@OptIn(UnstableApi::class)
@Composable
fun SelectiveCardNodeContainer(
    card: CardEntity,
    deltaState: State<NodeDelta>,
    governor: WebViewResourceGovernor,
    mediaPlaybackEngine: MediaPlaybackEngine,
    themeManager: ThemeManager,
    onCardDrag: (cardId: String, deltaX: Float, deltaY: Float) -> Unit,
    onCardDragEnd: (cardId: String) -> Unit,
    onFullscreenRequested: (cardId: String) -> Unit,
    onPiPRequested: (cardId: String) -> Unit,
    onOpenArticle: (url: String) -> Unit,
    onWakeRequested: (cardId: String) -> Unit,
    onMinimizeRequested: ((cardId: String) -> Unit)? = null,
    onCloseRequested: ((cardId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lifecycleEvents by governor.lifecycleEvents.collectAsState()
    val currentState = lifecycleEvents[card.cardId] ?: if (card.isHibernated) CardLifecycleState.MINIMIZED else CardLifecycleState.GRID_FLOW
    val isMinimized = currentState == CardLifecycleState.MINIMIZED || card.isHibernated

    Box(
        modifier = modifier
            .graphicsLayer {
                val delta = deltaState.value
                translationX = delta.offsetX
                translationY = delta.offsetY
                scaleX = delta.scaleX
                scaleY = delta.scaleY
                rotationZ = delta.rotationZ
                alpha = delta.alpha
            }
            .size(width = card.width.dp, height = if (isMinimized) 56.dp else card.height.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 32dp Header Bar for Window Management Gestures
                CardHeaderBar(
                    card = card,
                    isMinimized = isMinimized,
                    onCardDrag = { dx, dy -> onCardDrag(card.cardId, dx, dy) },
                    onCardDragEnd = { onCardDragEnd(card.cardId) },
                    onDoubleTapImmersive = { onFullscreenRequested(card.cardId) },
                    onMinimizeToggle = {
                        if (isMinimized) {
                            onWakeRequested(card.cardId)
                        } else {
                            onMinimizeRequested?.invoke(card.cardId) ?: governor.transitionTo(card.cardId, CardLifecycleState.MINIMIZED)
                        }
                    },
                    onFullscreen = { onFullscreenRequested(card.cardId) },
                    onClose = { onCloseRequested?.invoke(card.cardId) }
                )

                // Content Body (32dp+)
                if (isMinimized) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onWakeRequested(card.cardId) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "💤 Minimized (Tap to restore)",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (CardType.fromId(card.cardType)) {
                            CardType.WEB -> LiveCardFrame(card, governor, themeManager)
                            CardType.VIDEO -> VideoCardFrame(card, mediaPlaybackEngine, onFullscreenRequested, onPiPRequested)
                            CardType.ARTICLE -> ArticleCardFrame(card, onOpenArticle)
                            CardType.AUDIO -> AudioCardFrame(card, mediaPlaybackEngine)
                            CardType.IMAGE -> ImageCardFrame(card)
                            CardType.NATIVE, CardType.PDF -> StaticCardContent(card)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 32dp dedicated Window Header Bar.
 * Encapsulates window drag, double-tap fullscreen, minimize, and close gestures.
 */
@Composable
fun CardHeaderBar(
    card: CardEntity,
    isMinimized: Boolean,
    onCardDrag: (deltaX: Float, deltaY: Float) -> Unit,
    onCardDragEnd: () -> Unit,
    onDoubleTapImmersive: () -> Unit,
    onMinimizeToggle: () -> Unit,
    onFullscreen: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color(0xFF0F172A))
            .pointerInput(card.cardId) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTapImmersive() }
                )
            }
            .pointerInput(card.cardId) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onCardDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { onCardDragEnd() }
                )
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Card type badge and Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF334155))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = CardType.fromId(card.cardType).name.take(3),
                    color = Color(0xFF38BDF8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = card.contentPayload.substringAfterLast('/').ifEmpty { "Card #${card.cardId.take(4)}" },
                color = Color(0xFFF8FAFC),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        // Right: Window action buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Minimize button
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAB308))
                    .clickable { onMinimizeToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMinimized) "+" else "−",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Fullscreen / Immersive button
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .clickable { onFullscreen() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⤢", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Close button
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✕", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
