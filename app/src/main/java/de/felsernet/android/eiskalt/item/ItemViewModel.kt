package de.felsernet.android.eiskalt.item

import android.app.Application
import androidx.lifecycle.viewModelScope
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseViewModel
import de.felsernet.android.eiskalt.common.SharedMessageViewModel
import de.felsernet.android.eiskalt.group.Group
import de.felsernet.android.eiskalt.group.GroupRepository
import de.felsernet.android.eiskalt.inventory.ListInfo
import de.felsernet.android.eiskalt.settings.UserSettingsRepository
import kotlinx.coroutines.launch

class ItemViewModel(application: Application) : BaseViewModel<Item>(application) {

    private lateinit var itemRepository: ItemRepository
    override val repository get() = itemRepository
    override val typeNameRes = R.string.type_item
    override val typeNamePluralRes = R.string.type_items

    private lateinit var listId: String
    private var groupMap: Map<String, Group> = emptyMap()
    private var collapsedGroups: Set<String> = emptySet()

    @Deprecated("Use overloaded initialize instead")
    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        throw UnsupportedOperationException("Use overloaded initialize instead")
    }

    fun initialize(sharedMessageViewModel: SharedMessageViewModel, listInfo: ListInfo) {
        super.initialize(sharedMessageViewModel)
        this.itemRepository = ItemRepository(listInfo)
        // the ViewModel is shared by all lists: take the folded groups of this list
        listId = listInfo.id
        collapsedGroups = UserSettingsRepository.getCachedCollapsedGroups(listId)
    }

    /**
     * Override loadData to also fetch groups for grouping
     */
    override fun loadData() {
        super.loadData()
        viewModelScope.launch {
            try {
                val groupRepository = GroupRepository.getInstance()
                groupMap = groupRepository.getAll().associateBy { it.id }
                rebuildDisplayList()
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("db", getString(R.string.item_groups_load_error, e.message), e)
                reloadHouseholdIfAccessDenied(e)
            }
        }
        // the folded groups may be changed on another device
        viewModelScope.launch {
            collapsedGroups = UserSettingsRepository.refreshCollapsedGroups(listId)
            rebuildDisplayList()
        }
    }

    /**
     * Group the items by their groupId (see ItemGrouping)
     */
    override fun rebuildDisplayList() {
        _displayList.value = ItemGrouping.buildDisplayList(
            _list.value, groupMap, collapsedGroups, getString(R.string.no_group), getString(R.string.unknown_group))
    }

    fun toggleGroupExpansion(groupId: String) {
        val collapsed = !collapsedGroups.contains(groupId)
        if (collapsed)
            collapsedGroups = collapsedGroups + groupId
        else
            collapsedGroups = collapsedGroups - groupId

        UserSettingsRepository.setGroupCollapsed(listId, groupId, collapsed)
        rebuildDisplayList()
    }
}
