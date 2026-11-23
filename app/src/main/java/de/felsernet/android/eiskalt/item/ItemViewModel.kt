package de.felsernet.android.eiskalt.item

import android.app.Application
import androidx.lifecycle.viewModelScope
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseViewModel
import de.felsernet.android.eiskalt.common.SharedMessageViewModel
import de.felsernet.android.eiskalt.group.Group
import de.felsernet.android.eiskalt.group.GroupRepository
import de.felsernet.android.eiskalt.inventory.ListInfo
import de.felsernet.android.eiskalt.log.AppLog
import de.felsernet.android.eiskalt.settings.UserSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ItemViewModel(application: Application) : BaseViewModel<Item>(application) {

    private lateinit var itemRepository: ItemRepository
    override val repository get() = itemRepository
    override val messages = Messages(
        R.string.item_load_error,
        R.string.item_name_empty,
        R.string.item_save_error,
        R.string.item_delete_done,
        R.string.item_delete_error,
    )

    private lateinit var listId: String
    private var groupMap: Map<String, Group> = emptyMap()
    private var collapsedGroups: Set<String> = emptySet()
    // the recently deleted items of the list (templates for new items), newest first
    private lateinit var deletedItemRepository: DeletedItemRepository
    private var recentlyDeleted: List<Item> = emptyList()

    @Deprecated("Use overloaded initialize instead")
    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        throw UnsupportedOperationException("Use overloaded initialize instead")
    }

    fun initialize(sharedMessageViewModel: SharedMessageViewModel, listInfo: ListInfo) {
        super.initialize(sharedMessageViewModel)
        this.itemRepository = ItemRepository(listInfo)
        this.deletedItemRepository = DeletedItemRepository(listInfo)
        recentlyDeleted = emptyList()
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
            } catch (e: CancellationException) {
                throw e
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
        // only templates: on an error the list is shown without them
        viewModelScope.launch {
            try {
                recentlyDeleted = deletedItemRepository.getRecent()
                rebuildDisplayList()
            } catch (e: Exception) {
                AppLog.w("db", "Failed to load the recently deleted items", e)
            }
        }
    }

    // an item with the name of a recently deleted one exists again: the template is not needed anymore
    override fun onCreated(obj: Item) {
        val sameName = recentlyDeleted.filter { it.name.equals(obj.name, ignoreCase = true) }
        if (sameName.isEmpty())
            return

        recentlyDeleted = recentlyDeleted - sameName.toSet()
        rebuildDisplayList()
        viewModelScope.launch {
            try {
                sameName.forEach { deletedItemRepository.delete(it.id) }
            } catch (e: Exception) {
                AppLog.w("db", "Failed to remove the recently deleted item \"${obj.name}\"", e)
            }
        }
    }

    // remember the deleted item as template for new items
    override fun onDeleted(obj: Item) {
        viewModelScope.launch {
            try {
                recentlyDeleted = deletedItemRepository.add(obj)
                rebuildDisplayList()
            } catch (e: Exception) {
                AppLog.w("db", "Failed to remember the deleted item \"${obj.name}\"", e)
            }
        }
    }

    /**
     * Group the items by their groupId, the recently deleted ones at the end (see ItemGrouping)
     */
    override fun rebuildDisplayList() {
        _displayList.value = ItemGrouping.buildDisplayList(
            _list.value, recentlyDeleted, groupMap, collapsedGroups,
            getString(R.string.no_group), getString(R.string.unknown_group), getString(R.string.item_recently_deleted))
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
