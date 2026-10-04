package com.crescentdeck.ui.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crescentdeck.data.db.entity.TabGroupEntity

/**
 * Visual badge identifying a tab's group or category.
 */
@Composable
fun TabGroupChip(
    group: TabGroupEntity,
    modifier: Modifier = Modifier
) {
    val groupColor = Color(group.colorTag)

    Box(
        modifier = modifier
            .background(groupColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = group.label,
            color = groupColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
