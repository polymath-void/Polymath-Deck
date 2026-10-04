package com.crescentdeck.ui.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crescentdeck.data.db.entity.TabEntity

/**
 * Context menu actions for a tab (Close, Hibernate, Group).
 */
@Composable
fun TabContextMenu(
    expanded: Boolean,
    tab: TabEntity,
    onDismissRequest: () -> Unit,
    onCloseTab: () -> Unit,
    onHibernateTab: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        DropdownMenuItem(
            text = { Text("Hibernate Tab", color = Color(0xFF38BDF8), fontSize = 13.sp) },
            onClick = {
                onHibernateTab()
                onDismissRequest()
            }
        )

        Divider(color = Color(0xFF334155))

        DropdownMenuItem(
            text = { Text("Close Tab", color = Color(0xFFFB7185), fontSize = 13.sp) },
            onClick = {
                onCloseTab()
                onDismissRequest()
            }
        )
    }
}
