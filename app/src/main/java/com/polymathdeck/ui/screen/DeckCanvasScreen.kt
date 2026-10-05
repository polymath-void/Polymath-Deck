package com.polymathdeck.ui.screen

import android.app.Activity
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.polymathdeck.ui.canvas.DragDropGridCanvas
import com.polymathdeck.ui.intent.ViewIntent
import com.polymathdeck.ui.tab.TabStripRail
import com.polymathdeck.ui.theme.PolymathTheme
import com.polymathdeck.ui.theme.ThemeManager
import com.polymathdeck.viewmodel.DeckViewModel
import com.polymathdeck.viewmodel.FeedViewModel
import com.polymathdeck.viewmodel.MediaViewModel
import com.polymathdeck.viewmodel.TabViewModel

/**
 * Primary viewport screen integrating the horizontal Tab rail,
 * 2D spatial canvas, URL addition dialog, and theme switching.
 */
@OptIn(UnstableApi::class)
@Composable
fun DeckCanvasScreen(
    deckViewModel: DeckViewModel,
    tabViewModel: TabViewModel,
    feedViewModel: FeedViewModel,
    mediaViewModel: MediaViewModel,
    themeManager: ThemeManager,
    onLaunchFullscreen: (cardId: String) -> Unit,
    onOpenUrlExternal: (url: String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val activeTab by tabViewModel.activeTab.collectAsState()
    val tabs by tabViewModel.tabs.collectAsState()
    val cards by deckViewModel.cards.collectAsState()
    val currentTheme by themeManager.currentGlobalTheme.collectAsState()

    var showAddCardDialog by remember { mutableStateOf(false) }
    var inputUrl by remember { mutableStateOf("") }

    LaunchedEffect(activeTab?.deckId) {
        val targetDeckId = activeTab?.deckId ?: "default_deck"
        deckViewModel.loadDeck(targetDeckId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Horizontal Tab Strip
            TabStripRail(
                tabs = tabs,
                activeTabId = activeTab?.tabId,
                onTabSelected = { tabId, _ -> tabViewModel.selectTab(tabId) },
                onTabClosed = { tabId -> tabViewModel.closeTab(tabId) },
                onTabHibernated = { tabId -> tabViewModel.hibernateTab(tabId) },
                onNewTab = { tabViewModel.createNewTab("Deck ${tabs.size + 1}") }
            )

            // Top action controls bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF162032))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POLYMATH DECK",
                    color = Color(0xFF38BDF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${cards.size} Cards",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Theme switch button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .clickable {
                            val nextTheme = if (currentTheme.id == "dark_slate") {
                                PolymathTheme.EmeraldOasis
                            } else if (currentTheme.id == "emerald_oasis") {
                                PolymathTheme.CyberpunkRose
                            } else {
                                PolymathTheme.DefaultDark
                            }
                            themeManager.setGlobalTheme(nextTheme)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Theme: ${currentTheme.name}",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Add Card Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8))
                        .clickable { showAddCardDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Clear Deck Button
                if (cards.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                            .clickable {
                                deckViewModel.processIntent(ViewIntent.ClearAllNodes)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Clear Deck",
                            color = Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2D Physics Canvas
            DragDropGridCanvas(
                cards = cards,
                invalidationBridge = deckViewModel.invalidationBridge,
                governor = deckViewModel.governor,
                mediaPlaybackEngine = mediaViewModel.mediaPlaybackEngine,
                themeManager = themeManager,
                onCardDrag = { cardId, dx, dy -> deckViewModel.processIntent(ViewIntent.UpdatePosition(cardId, dx, dy)) },
                onCardDragEnd = { cardId -> deckViewModel.processIntent(ViewIntent.DragEnd(cardId)) },
                onFullscreenRequested = { cardId ->
                    deckViewModel.processIntent(ViewIntent.ToggleImmersive(cardId))
                    onLaunchFullscreen(cardId)
                },
                onPiPRequested = {
                    if (activity != null) {
                        mediaViewModel.enterPiP(activity)
                    }
                },
                onOpenArticle = { url -> onOpenUrlExternal(url) },
                onWakeRequested = { cardId -> deckViewModel.processIntent(ViewIntent.RestoreNode(cardId)) },
                onMinimizeRequested = { cardId -> deckViewModel.processIntent(ViewIntent.MinimizeNode(cardId)) },
                onCloseRequested = { cardId -> deckViewModel.processIntent(ViewIntent.RemoveNode(cardId)) },
                onCardResize = { cardId, w, h -> deckViewModel.processIntent(ViewIntent.UpdateSize(cardId, w, h)) },
                onCardResizeEnd = { cardId -> deckViewModel.processIntent(ViewIntent.ResizeEnd(cardId)) },
                onOpenUrlAsCard = { url -> deckViewModel.processIntent(ViewIntent.AddNodeFromUri(url)) },
                modifier = Modifier.weight(1f)
            )
        }

        // Add Card URL Dialog
        if (showAddCardDialog) {
            AlertDialog(
                onDismissRequest = { showAddCardDialog = false },
                containerColor = Color(0xFF1E293B),
                title = { Text("Add Card to Deck", color = Color.White, fontSize = 16.sp) },
                text = {
                    Column {
                        Text(
                            text = "Enter any Web URL, Video stream (.mp4/.m3u8), or RSS feed link:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text("https://...", color = Color(0xFF64748B)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputUrl.isNotBlank()) {
                                deckViewModel.processIntent(ViewIntent.AddNodeFromUri(inputUrl.trim()))
                                inputUrl = ""
                                showAddCardDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Text("Add Node", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCardDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}
