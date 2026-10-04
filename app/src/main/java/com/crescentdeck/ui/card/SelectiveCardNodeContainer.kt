package com.crescentdeck.ui.card

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.crescentdeck.data.db.entity.CardEntity
import com.crescentdeck.data.db.entity.CardType
import com.crescentdeck.engine.governor.WebViewResourceGovernor
import com.crescentdeck.engine.media.MediaPlaybackEngine
import com.crescentdeck.engine.physics.NodeDelta
import com.crescentdeck.ui.theme.ThemeManager

/**
 * SelectiveCardNodeContainer hosts individual cards on the GPU render layer.
 *
 * It reads NodeDelta state directly inside Modifier.graphicsLayer lambda,
 * executing hardware transformations entirely on the GPU without triggering
 * Compose measure or layout recompositions.
 */
@OptIn(UnstableApi::class)
@Composable
fun SelectiveCardNodeContainer(
    card: CardEntity,
    deltaState: State<NodeDelta>,
    governor: WebViewResourceGovernor,
    mediaPlaybackEngine: MediaPlaybackEngine,
    themeManager: ThemeManager,
    onFullscreenRequested: (cardId: String) -> Unit,
    onPiPRequested: (cardId: String) -> Unit,
    onOpenArticle: (url: String) -> Unit,
    onWakeRequested: (cardId: String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            .size(width = card.width.dp, height = card.height.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            shadowElevation = 8.dp
        ) {
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
        }
    }
}
