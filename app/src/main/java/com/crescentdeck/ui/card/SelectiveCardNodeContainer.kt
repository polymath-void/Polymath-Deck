package com.crescentdeck.ui.card

import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Implements "Tactile Spatial Computing" Design Language:
 * - Dynamic Z-Axis Elevation: 6dp resting -> 24dp active drag heavy detached shadow.
 * - Acrylic Translucency: 90% opacity on drag allowing underlying cards to show.
 * - Traffic Lights Header Bar: macOS/Wayland style close (red), minimize (yellow), immersive (green) pills.
 * - Interactive Corner Resize Handle with real-time numerical dimension overlay badge (e.g. 1200x800).
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
    onCardResize: ((cardId: String, newWidth: Float, newHeight: Float) -> Unit)? = null,
    onCardResizeEnd: ((cardId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lifecycleEvents by governor.lifecycleEvents.collectAsState()
    val currentState = lifecycleEvents[card.cardId]
        ?: if (card.isHibernated) CardLifecycleState.MINIMIZED else CardLifecycleState.GRID_FLOW

    val isMinimized = currentState == CardLifecycleState.MINIMIZED || card.isHibernated
    val isImmersive = currentState == CardLifecycleState.IMMERSIVE
    val isResizing = currentState == CardLifecycleState.RESIZING

    val delta = deltaState.value
    val isBeingDragged = delta.scaleX > 1.02f

    var currentWidth by remember(card.width) { mutableFloatStateOf(card.width) }
    var currentHeight by remember(card.height) { mutableFloatStateOf(card.height) }
    var isInteractingWithResize by remember { mutableStateOf(false) }

    // Dynamic Z-Axis elevation and physical properties
    val elevation = when {
        isBeingDragged -> 24.dp
        isResizing || isInteractingWithResize -> 16.dp
        isImmersive -> 0.dp
        else -> 6.dp
    }

    val cornerRadius = if (isImmersive) 0.dp else 16.dp
    val acrylicAlpha = if (isBeingDragged) 0.90f else delta.alpha

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = delta.offsetX
                translationY = delta.offsetY
                scaleX = if (isBeingDragged) 1.04f else delta.scaleX
                scaleY = if (isBeingDragged) 1.04f else delta.scaleY
                rotationZ = delta.rotationZ
                alpha = acrylicAlpha
            }
            .size(
                width = currentWidth.dp,
                height = if (isMinimized) 56.dp else currentHeight.dp
            )
    ) {
        Surface(
            shape = RoundedCornerShape(cornerRadius),
            color = Color(0xFF1E293B),
            shadowElevation = elevation,
            border = if (isResizing || isInteractingWithResize) {
                BorderStroke(2.dp, Color(0xFF38BDF8))
            } else if (isBeingDragged) {
                BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
            } else null
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 32dp Header Bar with Traffic Lights & Grab Handle
                CardHeaderBar(
                    card = card,
                    isMinimized = isMinimized,
                    isBeingDragged = isBeingDragged,
                    onCardDrag = { dx, dy -> onCardDrag(card.cardId, dx, dy) },
                    onCardDragEnd = { onCardDragEnd(card.cardId) },
                    onDoubleTapImmersive = { onFullscreenRequested(card.cardId) },
                    onMinimizeToggle = {
                        if (isMinimized) {
                            onWakeRequested(card.cardId)
                        } else {
                            onMinimizeRequested?.invoke(card.cardId)
                                ?: governor.transitionTo(card.cardId, CardLifecycleState.MINIMIZED)
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
                        if (card.isHibernated) {
                            HibernationScrimOverlay(
                                snapshot = governor.getSnapshot(card.cardId),
                                onWakeRequested = { onWakeRequested(card.cardId) }
                            )
                        } else {
                            when (CardType.fromId(card.cardType)) {
                                CardType.WEB -> LiveCardFrame(card, governor, themeManager)
                                CardType.VIDEO -> VideoCardFrame(card, mediaPlaybackEngine, onFullscreenRequested, onPiPRequested)
                                CardType.ARTICLE -> ArticleCardFrame(card, onOpenArticle)
                                CardType.AUDIO -> AudioCardFrame(card, mediaPlaybackEngine)
                                CardType.IMAGE -> ImageCardFrame(card)
                                CardType.NATIVE, CardType.PDF -> StaticCardContent(card)
                            }
                        }

                        // Corner Resize Handle (Bottom-Right)
                        if (!isMinimized && !isImmersive) {
                            CornerResizeHandle(
                                onResize = { dx, dy ->
                                    isInteractingWithResize = true
                                    currentWidth = (currentWidth + dx).coerceIn(200f, 1400f)
                                    currentHeight = (currentHeight + dy).coerceIn(140f, 1000f)
                                    onCardResize?.invoke(card.cardId, currentWidth, currentHeight)
                                },
                                onResizeEnd = {
                                    isInteractingWithResize = false
                                    onCardResizeEnd?.invoke(card.cardId)
                                },
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }

                        // Real-Time Numerical Dimension Badge Overlay during resize
                        if (isInteractingWithResize || isResizing) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.85f))
                                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${currentWidth.toInt()} × ${currentHeight.toInt()}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 32dp dedicated Window Header Bar.
 * Left: Traffic lights (Close #FF5F56, Minimize #FFBD2E, Immersive #27C93F).
 * Center: Favicon and truncated title.
 * Right: Subtle grab handle texture (···).
 */
@Composable
fun CardHeaderBar(
    card: CardEntity,
    isMinimized: Boolean,
    isBeingDragged: Boolean,
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
            .background(if (isBeingDragged) Color(0xFF1E293B) else Color(0xFF0F172A))
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
        // Left: Traffic Light Control Pills
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Close Traffic Light (Red)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5F56))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✕", color = Color(0xFF4C0000), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }

            // Minimize Traffic Light (Yellow)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFBD2E))
                    .clickable { onMinimizeToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "−", color = Color(0xFF5E4100), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Fullscreen / Immersive Traffic Light (Green)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF27C93F))
                    .clickable { onFullscreen() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = Color(0xFF003808), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Center: Title of the webpage / card
        Text(
            text = card.contentPayload.substringAfterLast('/').ifEmpty { "Card #${card.cardId.take(4)}" },
            color = Color(0xFFF8FAFC),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        )

        // Right: Grab Handle Texture (Three Horizontal Dots)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(end = 4.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF64748B))
                )
            }
        }
    }
}

/**
 * Draggable bottom-right corner resize handle.
 */
@Composable
fun CornerResizeHandle(
    onResize: (deltaX: Float, deltaY: Float) -> Unit,
    onResizeEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onResize(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { onResizeEnd() }
                )
            }
            .padding(4.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Diagonal grip lines indicator
        Text(
            text = "⌟",
            color = Color(0xFF94A3B8),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
