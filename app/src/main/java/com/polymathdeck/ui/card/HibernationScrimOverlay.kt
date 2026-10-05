package com.polymathdeck.ui.card

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual overlay displayed when a card's heavy WebView or media context
 * has been hibernated to save device RAM.
 *
 * Implements the "Cold Start Veil":
 * - Desaturating grayscale filter over the hardware bitmap snapshot.
 * - Tap-to-wake activates a progressive loading veil with a circular spinner,
 *   gracefully masking the background process re-spawning delay.
 */
@Composable
fun HibernationScrimOverlay(
    snapshot: Bitmap?,
    onWakeRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isWaking by remember { mutableStateOf(false) }

    val desaturationFilter = remember {
        val matrix = ColorMatrix().apply { setToSaturation(0.15f) }
        ColorFilter.colorMatrix(matrix)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable {
                if (!isWaking) {
                    isWaking = true
                    onWakeRequested()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (snapshot != null && !snapshot.isRecycled) {
            Image(
                bitmap = snapshot.asImageBitmap(),
                contentDescription = "Hibernated snapshot",
                contentScale = ContentScale.Crop,
                colorFilter = desaturationFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (isWaking) 16.dp else 6.dp)
            )
        }

        // Dark dimming scrim with acrylic feel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A).copy(alpha = if (isWaking) 0.85f else 0.70f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            if (isWaking) {
                CircularProgressIndicator(
                    color = Color(0xFF38BDF8),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Restoring live process...",
                    color = Color.White,
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.labelMedium
                )
            } else {
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFF38BDF8).copy(alpha = 0.18f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "💤 HIBERNATED (RAM SAVED)",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap to resume live session",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 13.sp
                )
            }
        }
    }
}
