package com.crescentdeck.ui.canvas

import androidx.annotation.OptIn
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.crescentdeck.bridge.NodeSelectiveInvalidationState
import com.crescentdeck.data.db.entity.CardEntity
import com.crescentdeck.engine.governor.WebViewResourceGovernor
import com.crescentdeck.engine.media.MediaPlaybackEngine
import com.crescentdeck.ui.card.SelectiveCardNodeContainer
import com.crescentdeck.ui.theme.ThemeManager
import kotlin.math.abs

/**
 * 2D Infinite Canvas rendering spatial cards with GPU-accelerated graphicsLayer transformations.
 *
 * Implements "Tactile Spatial Computing" Canvas Engine:
 * - Procedural Infinite Dot Grid: Subtle 32dp dot grid shifting with pan/zoom (visual anchor).
 * - Ambient Light Sampling: Radial gradient glow subtly reflecting the focused card/theme accent.
 * - Elastic Edge Resistance: Physical rubber-banding when panning against world boundaries.
 * - Magnetic Drop Zone Highlight: Outlines the physical anchor gap when a card is in flight.
 */
@OptIn(UnstableApi::class)
@Composable
fun DragDropGridCanvas(
    cards: List<CardEntity>,
    invalidationBridge: NodeSelectiveInvalidationState,
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
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    val currentTheme by themeManager.currentGlobalTheme.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 1. Ambient Light Glow (Radial Gradient)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            currentTheme.accentPrimary.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.75f
                    )
                )

                // 2. Procedural Infinite Dot Grid (Visual Anchor)
                val dotSpacing = 32.dp.toPx() * scale
                val dotRadius = (1.5.dp.toPx() * scale).coerceIn(1f, 3.5f)
                val dotColor = Color(0xFF334155).copy(alpha = 0.35f)

                val startX = (panX % dotSpacing) - dotSpacing
                val startY = (panY % dotSpacing) - dotSpacing

                var x = startX
                while (x < size.width + dotSpacing) {
                    var y = startY
                    while (y < size.height + dotSpacing) {
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += dotSpacing
                    }
                    x += dotSpacing
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.3f, 2.5f)
                    // Elastic Edge Rubber-Banding
                    val resistanceX = 1f + (abs(panX) * 0.0003f)
                    val resistanceY = 1f + (abs(panY) * 0.0003f)
                    panX += pan.x / resistanceX
                    panY += pan.y / resistanceY
                }
            }
            .graphicsLayer {
                translationX = panX
                translationY = panY
                scaleX = scale
                scaleY = scale
            }
    ) {
        // Draw Magnetic Drop Zone outlines at resting anchor points
        for (card in cards) {
            Box(
                modifier = Modifier
                    .offset(x = card.anchorX.dp, y = card.anchorY.dp)
                    .size(width = card.width.dp, height = card.height.dp)
                    .border(
                        width = 1.dp,
                        color = Color(0xFF38BDF8).copy(alpha = 0.20f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

        // Render Spatial Card Nodes
        for (card in cards) {
            val deltaFlow = remember(card.cardId) {
                invalidationBridge.getDeltaFlow(card.cardId)
            }
            val deltaState = deltaFlow.collectAsState()

            SelectiveCardNodeContainer(
                card = card,
                deltaState = deltaState,
                governor = governor,
                mediaPlaybackEngine = mediaPlaybackEngine,
                themeManager = themeManager,
                onCardDrag = onCardDrag,
                onCardDragEnd = onCardDragEnd,
                onFullscreenRequested = onFullscreenRequested,
                onPiPRequested = onPiPRequested,
                onOpenArticle = onOpenArticle,
                onWakeRequested = onWakeRequested,
                onMinimizeRequested = onMinimizeRequested,
                onCloseRequested = onCloseRequested,
                onCardResize = onCardResize,
                onCardResizeEnd = onCardResizeEnd,
                modifier = Modifier.offset(x = card.anchorX.dp, y = card.anchorY.dp)
            )
        }
    }
}
