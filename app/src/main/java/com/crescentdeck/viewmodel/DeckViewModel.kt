package com.crescentdeck.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crescentdeck.bridge.NodeSelectiveInvalidationState
import com.crescentdeck.bridge.PhysicsToComposeDispatcher
import com.crescentdeck.data.db.entity.CardEntity
import com.crescentdeck.data.db.entity.CardType
import com.crescentdeck.data.db.entity.DeckEntity
import com.crescentdeck.data.repository.DeckRepository
import com.crescentdeck.engine.governor.WebViewResourceGovernor
import com.crescentdeck.engine.quadtree.CardNode
import com.crescentdeck.engine.quadtree.QuadTreePhysicsEngine
import com.crescentdeck.engine.router.ContentRouter
import com.crescentdeck.recovery.CrashRecoveryManager
import dagger.hilt.android.lifecycle.HiltViewModel
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

    init {
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

            // Attempt crash recovery restoration
            val restored = recoveryManager.attemptRestore(deckId)

            deckRepository.getCardsForDeckFlow(deckId).onEach { cardEntities ->
                _cards.value = cardEntities

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

    fun onCardDrag(cardId: String, deltaX: Float, deltaY: Float) {
        val node = quadTreeEngine.locateNode(cardId) ?: return
        node.isBeingDragged = true
        node.x += deltaX
        node.y += deltaY
        quadTreeEngine.updateNodePosition(cardId, node.x, node.y)
    }

    fun onCardDragEnd(cardId: String) {
        val node = quadTreeEngine.locateNode(cardId) ?: return
        node.isBeingDragged = false
        viewModelScope.launch {
            deckRepository.updateCardPosition(cardId, node.x, node.y)
        }
    }

    /**
     * Resolves incoming URL via ContentRouter and adds a new card to the active deck.
     */
    fun addCardFromUri(url: String) {
        val deck = _currentDeck.value ?: return
        viewModelScope.launch {
            val resolvedType = contentRouter.resolve(Uri.parse(url))
            val currentCount = _cards.value.size
            val posX = 50f + (currentCount % 3) * 340f
            val posY = 100f + (currentCount / 3) * 280f

            val newCard = CardEntity(
                cardId = UUID.randomUUID().toString(),
                deckId = deck.deckId,
                cardType = resolvedType.typeId,
                anchorX = posX,
                anchorY = posY,
                contentPayload = url
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

    fun hibernateCard(cardId: String) {
        governor.hibernateCard(cardId)
        viewModelScope.launch {
            deckRepository.updateCardHibernation(cardId, true)
        }
    }

    fun wakeCard(cardId: String) {
        governor.wakeCard(cardId)
        viewModelScope.launch {
            deckRepository.updateCardHibernation(cardId, false)
        }
    }

    fun removeCard(cardId: String) {
        quadTreeEngine.removeNode(cardId)
        invalidationBridge.removeCard(cardId)
        governor.unregisterWebView(cardId)
        viewModelScope.launch {
            deckRepository.deleteCard(cardId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        physicsDispatcher.stopSimulation()
        recoveryManager.stopHeartbeat()
    }
}
