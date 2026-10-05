package com.polymathdeck.ui.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polymathdeck.data.db.entity.TabEntity

/**
 * Horizontal rail rendering interactive deck tabs.
 */
@Composable
fun TabStripRail(
    tabs: List<TabEntity>,
    activeTabId: String?,
    onTabSelected: (tabId: String, deckId: String) -> Unit,
    onTabClosed: (tabId: String) -> Unit,
    onTabHibernated: (tabId: String) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF0F172A))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(tabs, key = { it.tabId }) { tab ->
                    val isActive = tab.tabId == activeTabId
                    var showMenu by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isActive) Color(0xFF1E293B) else Color(0xFF162032)
                            )
                            .clickable { onTabSelected(tab.tabId, tab.deckId) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tab.title,
                                color = if (isActive) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "×",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp,
                                modifier = Modifier.clickable { onTabClosed(tab.tabId) }
                            )
                        }

                        TabContextMenu(
                            expanded = showMenu,
                            tab = tab,
                            onDismissRequest = { showMenu = false },
                            onCloseTab = { onTabClosed(tab.tabId) },
                            onHibernateTab = { onTabHibernated(tab.tabId) }
                        )
                    }
                }
            }

            // New Tab (+) Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                    .clickable { onNewTab() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = Color(0xFF38BDF8),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
