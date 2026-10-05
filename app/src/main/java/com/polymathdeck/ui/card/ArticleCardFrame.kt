package com.polymathdeck.ui.card

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.polymathdeck.data.db.entity.CardEntity
import org.json.JSONObject

/**
 * Clean reader-optimized card frame for RSS and Atom feed articles.
 */
@Composable
fun ArticleCardFrame(
    card: CardEntity,
    onOpenArticle: (url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val articleData = remember(card.contentPayload) {
        try {
            val json = JSONObject(card.contentPayload)
            object {
                val title = json.optString("title", "Untitled Article")
                val author = json.optString("author", "Unknown")
                val summary = json.optString("summary", "")
                val thumbnail = json.optString("thumbnail", "")
                val link = json.optString("link", "")
            }
        } catch (e: Exception) {
            object {
                val title = "Feed Article"
                val author = ""
                val summary = card.contentPayload
                val thumbnail = ""
                val link = ""
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .clickable {
                if (articleData.link.isNotBlank()) {
                    onOpenArticle(articleData.link)
                }
            }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (articleData.thumbnail.isNotBlank()) {
                    AsyncImage(
                        model = articleData.thumbnail,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(start = if (articleData.thumbnail.isNotBlank()) 10.dp else 0.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = articleData.title,
                        color = Color(0xFFF8FAFC),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (articleData.author.isNotBlank()) {
                        Text(
                            text = "by ${articleData.author}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = articleData.summary,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RSS ARTICLE",
                    color = Color(0xFFFB7185),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Tap to read →",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp
                )
            }
        }
    }
}
