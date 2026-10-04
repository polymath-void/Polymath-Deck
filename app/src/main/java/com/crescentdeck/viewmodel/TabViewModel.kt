package com.crescentdeck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crescentdeck.bridge.TabDeckState
import com.crescentdeck.data.db.dao.TabDao
import com.crescentdeck.data.db.entity.DeckEntity
import com.crescentdeck.data.db.entity.TabEntity
import com.crescentdeck.data.repository.DeckRepository
import com.crescentdeck.engine.governor.WebViewResourceGovernor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel managing browser-style deck tabs, group categorization, and tab hibernation.
 */
@HiltViewModel
class TabViewModel @Inject constructor(
    private val tabDao: TabDao,
    private val deckRepository: DeckRepository,
    private val governor: WebViewResourceGovernor,
    val tabDeckState: TabDeckState
) : ViewModel() {

    val tabs: StateFlow<List<TabEntity>> = tabDao.getAllTabsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeTab: StateFlow<TabEntity?> = tabDao.getActiveTabFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        // Sync tabs into TabDeckState
        viewModelScope.launch {
            combine(tabDao.getAllTabsFlow(), tabDao.getActiveTabFlow(), tabDao.getAllGroupsFlow()) { tabs, active, groups ->
                tabDeckState.updateTabs(tabs, active, groups)
            }.collect {}
        }
    }

    fun createNewTab(title: String = "New Canvas") {
        viewModelScope.launch {
            val newDeckId = UUID.randomUUID().toString()
            val newTabId = UUID.randomUUID().toString()

            val deck = DeckEntity(deckId = newDeckId, title = title)
            deckRepository.upsertDeck(deck)

            val tab = TabEntity(
                tabId = newTabId,
                deckId = newDeckId,
                title = title,
                isActive = true
            )
            tabDao.upsertTab(tab)
            tabDao.setActiveTab(newTabId)
        }
    }

    fun selectTab(tabId: String) {
        viewModelScope.launch {
            tabDeckState.setSwitching(true)
            tabDao.setActiveTab(tabId)
            tabDeckState.setSwitching(false)
        }
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch {
            val tab = tabDao.getTabById(tabId) ?: return@launch
            // Hibernate cards for closing tab
            val cards = deckRepository.getCardsForDeck(tab.deckId)
            for (card in cards) {
                governor.hibernateCard(card.cardId)
            }
            tabDao.deleteTabById(tabId)
        }
    }

    fun hibernateTab(tabId: String) {
        viewModelScope.launch {
            val tab = tabDao.getTabById(tabId) ?: return@launch
            val cards = deckRepository.getCardsForDeck(tab.deckId)
            for (card in cards) {
                governor.hibernateCard(card.cardId)
            }
            tabDao.updateTabHibernation(tabId, true)
        }
    }
}
