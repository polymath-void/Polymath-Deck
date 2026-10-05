package com.polymathdeck.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polymathdeck.bridge.NodeSelectiveInvalidationState
import com.polymathdeck.bridge.PhysicsToComposeDispatcher
import com.polymathdeck.data.db.entity.CardEntity
import com.polymathdeck.data.db.entity.CardType
import com.polymathdeck.data.db.entity.DeckEntity
import com.polymathdeck.data.repository.DeckRepository
import com.polymathdeck.engine.governor.CardLifecycleState
import com.polymathdeck.engine.governor.WebViewResourceGovernor
import com.polymathdeck.engine.quadtree.CardNode
import com.polymathdeck.engine.quadtree.QuadTreePhysicsEngine
import com.polymathdeck.engine.router.ContentRouter
import com.polymathdeck.recovery.CrashRecoveryManager
import com.polymathdeck.ui.intent.DeckViewState
import com.polymathdeck.ui.intent.ViewIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Main ViewModel driving the 2D spatial canvas deck, card states,
 * and physics simulation integration.
 */
@HiltViewModel
class DeckViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    val quadTreeEngine: QuadTreePhysicsEngine,
    private val physicsDispatcher: PhysicsToComposeDispatcher,
    val invalidationBridge: NodeSelectiveInvalidationState,
    val governor: WebViewResourceGovernor,
    private val recoveryManager: CrashRecoveryManager,
    private val contentRouter: ContentRouter
) : ViewModel() {

    private val _currentDeck = MutableStateFlow<DeckEntity?>(null)
    val currentDeck: StateFlow<DeckEntity?> = _currentDeck.asStateFlow()

    private val _cards = MutableStateFlow<List<CardEntity>>(emptyList())
    val cards: StateFlow<List<CardEntity>> = _cards.asStateFlow()

    private val _viewState = MutableStateFlow(DeckViewState())
    val viewState: StateFlow<DeckViewState> = _viewState.asStateFlow()

    init {
        // Wire governor to spatial QuadTree
        governor.quadTreeEngine = quadTreeEngine
        // Start high-frequency physics simulation loop
        physicsDispatcher.startSimulation(viewModelScope)
    }

    /**
     * Loads a deck, attempts crash restoration, and registers its cards in the spatial QuadTree.
     */
    fun loadDeck(deckId: String) {
        viewModelScope.launch {
            var deck = deckRepository.getDeckById(deckId)
            if (deck == null) {
                deck = DeckEntity(deckId = deckId, title = "Main Canvas")
                deckRepository.upsertDeck(deck)
            }
            _currentDeck.value = deck
            _viewState.value = _viewState.value.copy(currentDeck = deck, isLoading = false)

            // Attempt crash recovery restoration
            val restored = recoveryManager.attemptRestore(deckId)

            deckRepository.getCardsForDeckFlow(deckId).onEach { cardEntities ->
                _cards.value = cardEntities
                _viewState.value = _viewState.value.copy(cards = cardEntities)

                if (!restored) {
                    // Populate QuadTree with initial positions from Room
                    val spatialNodes = cardEntities.map { entity ->
                        CardNode(
                            id = entity.cardId,
                            x = entity.anchorX,
                            y = entity.anchorY,
                            width = entity.width,
                            height = entity.height,
                            anchorX = entity.anchorX,
                            anchorY = entity.anchorY,
                            priority = entity.priority,
                            cardType = entity.cardType
                        )
                    }
                    quadTreeEngine.batchInsert(spatialNodes)
                }
            }.launchIn(viewModelScope)

            // Start 5-second checkpoint heartbeat
            recoveryManager.startHeartbeat(viewModelScope, deckId)
        }
    }

    /**
     * Unified Model-View-Intent (MVI) entry point. Dispatches all user actions
     * to the spatial engine, resource governor, and reactive ViewState.
     */
    fun processIntent(intent: ViewIntent) {
        when (intent) {
            is ViewIntent.DragStart -> {
                val node = quadTreeEngine.locateNode(intent.nodeId)
                if (node != null) {
                    node.isBeingDragged = true
                    node.isSleeping = false
                }
                _viewState.value = _viewState.value.copy(focusedCardId = intent.nodeId)
            }
            is ViewIntent.UpdatePosition -> {
                onCardDrag(intent.nodeId, intent.deltaX, intent.deltaY)
            }
            is ViewIntent.DragEnd -> {
                onCardDragEnd(intent.nodeId)
            }
            is ViewIntent.ResizeStart -> {
                val node = quadTreeEngine.locateNode(intent.nodeId)
                if (node != null) {
                    node.isSleeping = false
                }
                governor.transitionTo(intent.nodeId, CardLifecycleState.RESIZING)
                _viewState.value = _viewState.value.copy(focusedCardId = intent.nodeId)
            }
            is ViewIntent.UpdateSize -> {
                onCardResize(intent.nodeId, intent.newWidth, intent.newHeight)
            }
            is ViewIntent.ResizeEnd -> {
                onCardResizeEnd(intent.nodeId)
            }
            is ViewIntent.ToggleImmersive -> {
                val current = governor.lifecycleEvents.value[intent.nodeId]
                if (current == CardLifecycleState.IMMERSIVE) {
                    governor.transitionTo(intent.nodeId, CardLifecycleState.GRID_FLOW)
                } else {
                    governor.transitionTo(intent.nodeId, CardLifecycleState.IMMERSIVE)
                }
            }
            is ViewIntent.MinimizeNode -> {
                hibernateCard(intent.nodeId)
            }
            is ViewIntent.RestoreNode -> {
                wakeCard(intent.nodeId)
            }
            is ViewIntent.AddNodeFromUri -> {
                addCardFromUri(intent.uri)
            }
            is ViewIntent.RemoveNode -> {
                removeCard(intent.nodeId)
                if (_viewState.value.focusedCardId == intent.nodeId) {
                    _viewState.value = _viewState.value.copy(focusedCardId = null)
                }
            }
            is ViewIntent.ClearAllNodes -> {
                clearAllCards()
            }
            is ViewIntent.PanViewport -> {
                _viewState.value = _viewState.value.copy(
                    viewportPanX = _viewState.value.viewportPanX + intent.deltaX,
                    viewportPanY = _viewState.value.viewportPanY + intent.deltaY
                )
            }
            is ViewIntent.ZoomViewport -> {
                val newScale = (_viewState.value.viewportScale * intent.zoomFactor).coerceIn(0.2f, 3.0f)
                _viewState.value = _viewState.value.copy(viewportScale = newScale)
            }
        }
    }

    fun onCardDrag(cardId: String, deltaX: Float, deltaY: Float) {
        val node = quadTreeEngine.locateNode(cardId) ?: return
        node.isBeingDragged = true
        node.isSleeping = false
        node.x += deltaX
        node.y += deltaY
        quadTreeEngine.updateNodePosition(cardId, node.x, node.y)
    }

    fun onCardDragEnd(cardId: String) {
        val node = quadTreeEngine.locateNode(cardId) ?: return
        node.isBeingDragged = false
        viewModelScope.launch(Dispatchers.IO) {
            deckRepository.updateCardPosition(cardId, node.x, node.y)
        }
    }

    fun onCardResize(cardId: String, newWidth: Float, newHeight: Float) {
        val node = quadTreeEngine.locateNode(cardId) ?: return
        node.width = newWidth.coerceIn(180f, 1600f)
        node.height = newHeight.coerceIn(120f, 1200f)
        node.isSleeping = false
        quadTreeEngine.forceRebuild()
        governor.transitionTo(cardId, CardLifecycleState.RESIZING)
        viewModelScope.launch(Dispatchers.IO) {
            val existing = _cards.value.find { it.cardId == cardId }
            if (existing != null) {
                val updated = existing.copy(width = node.width, height = node.height)
                deckRepository.upsertCard(updated)
            }
        }
    }

    fun onCardResizeEnd(cardId: String) {
        governor.transitionTo(cardId, CardLifecycleState.GRID_FLOW)
    }

    private var lastSpawnTime: Long = 0L
    private var lastSpawnUrl: String? = null

    /**
     * Resolves incoming URL via ContentRouter and adds a new card to the active deck.
     */
    fun addCardFromUri(url: String) {
        val deck = _currentDeck.value ?: return
        val raw = url.trim()
        if (raw.isEmpty()) return

        val normalizedUrl = if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
            raw
        } else {
            "https://$raw"
        }

        // Deduplication & rapid spawn guard (debounce within 1.5s for same URL)
        val now = System.currentTimeMillis()
        if (normalizedUrl == lastSpawnUrl && now - lastSpawnTime < 1500) {
            return
        }
        lastSpawnUrl = normalizedUrl
        lastSpawnTime = now

        viewModelScope.launch(Dispatchers.IO) {
            // Check if exact card already exists in the active deck
            val existing = _cards.value.find { it.contentPayload.trim().equals(normalizedUrl, ignoreCase = true) }
            if (existing != null) {
                _viewState.value = _viewState.value.copy(focusedCardId = existing.cardId)
                return@launch
            }

            val resolvedType = contentRouter.resolve(Uri.parse(normalizedUrl))
            val currentCount = _cards.value.size
            val posX = 50f + (currentCount % 3) * 340f
            val posY = 100f + (currentCount / 3) * 280f

            val newCard = CardEntity(
                cardId = UUID.randomUUID().toString(),
                deckId = deck.deckId,
                cardType = resolvedType.typeId,
                anchorX = posX,
                anchorY = posY,
                contentPayload = normalizedUrl
            )

            deckRepository.upsertCard(newCard)
            quadTreeEngine.insertNode(
                CardNode(
                    id = newCard.cardId,
                    x = posX,
                    y = posY,
                    width = newCard.width,
                    height = newCard.height,
                    anchorX = posX,
                    anchorY = posY,
                    cardType = newCard.cardType
                )
            )
        }
    }

    fun clearAllCards() {
        val deck = _currentDeck.value ?: return
        val currentCards = _cards.value
        currentCards.forEach { card ->
            quadTreeEngine.removeNode(card.cardId)
            invalidationBridge.removeCard(card.cardId)
            governor.unregisterWebView(card.cardId)
        }
        viewModelScope.launch(Dispatchers.IO) {
            deckRepository.deleteCardsForDeck(deck.deckId)
        }
        _viewState.value = _viewState.value.copy(focusedCardId = null)
    }

    fun hibernateCard(cardId: String) {
        governor.hibernateCard(cardId)
        viewModelScope.launch(Dispatchers.IO) {
            deckRepository.updateCardHibernation(cardId, true)
        }
    }

    fun wakeCard(cardId: String) {
        governor.wakeCard(cardId)
        viewModelScope.launch(Dispatchers.IO) {
            deckRepository.updateCardHibernation(cardId, false)
        }
    }

    fun removeCard(cardId: String) {
        quadTreeEngine.removeNode(cardId)
        invalidationBridge.removeCard(cardId)
        governor.unregisterWebView(cardId)
        viewModelScope.launch(Dispatchers.IO) {
            deckRepository.deleteCard(cardId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        physicsDispatcher.stopSimulation()
        recoveryManager.stopHeartbeat()
    }
}
