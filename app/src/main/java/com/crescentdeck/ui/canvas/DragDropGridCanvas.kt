package com.crescentdeck.ui.canvas

import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

/**
 * 2D Infinite Canvas rendering spatial cards with GPU-accelerated graphicsLayer transformations.
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
    modifier: Modifier = Modifier
) {
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.3f, 2.5f)
                    panX += pan.x
                    panY += pan.y
                }
            }
            .graphicsLayer {
                translationX = panX
                translationY = panY
                scaleX = scale
                scaleY = scale
            }
    ) {
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
                onFullscreenRequested = onFullscreenRequested,
                onPiPRequested = onPiPRequested,
                onOpenArticle = onOpenArticle,
                onWakeRequested = onWakeRequested,
                modifier = Modifier
                    .offset(x = card.anchorX.dp, y = card.anchorY.dp)
                    .pointerInput(card.cardId) {
                        detectTransformGestures { _, pan, _, _ ->
                            onCardDrag(card.cardId, pan.x, pan.y)
                        }
                    }
            )
        }
    }
}
