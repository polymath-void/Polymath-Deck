package com.crescentdeck.bridge

import com.crescentdeck.data.db.entity.TabEntity
import com.crescentdeck.data.db.entity.TabGroupEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class TabManagerState(
    val activeTab: TabEntity? = null,
    val tabs: List<TabEntity> = emptyList(),
    val groups: List<TabGroupEntity> = emptyList(),
    val isSwitching: Boolean = false
)

/**
 * Reactive bridge for tab switching, grouping, and tab strip presentation state.
 */
@Singleton
class TabDeckState @Inject constructor() {

    private val _tabState = MutableStateFlow(TabManagerState())
    val tabState: StateFlow<TabManagerState> = _tabState.asStateFlow()

    fun updateTabs(tabs: List<TabEntity>, active: TabEntity?, groups: List<TabGroupEntity>) {
        _tabState.value = _tabState.value.copy(
            tabs = tabs,
            activeTab = active,
            groups = groups
        )
    }

    fun setSwitching(switching: Boolean) {
        _tabState.value = _tabState.value.copy(isSwitching = switching)
    }
}
