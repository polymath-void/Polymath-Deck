package com.polymathdeck.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polymathdeck.data.db.entity.CardEntity

/**
 * Standard native Compose content card.
 */
@Composable
fun StaticCardContent(
    card: CardEntity,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Card Node #${card.cardId.take(6)}",
                color = Color(0xFF38BDF8),
                fontSize = 14.sp,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = card.contentPayload.ifBlank { "Static Native Card Workspace Item" },
                color = Color(0xFFF8FAFC),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Priority: ${card.priority} | Spatial: (${card.anchorX.toInt()}, ${card.anchorY.toInt()})",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}
