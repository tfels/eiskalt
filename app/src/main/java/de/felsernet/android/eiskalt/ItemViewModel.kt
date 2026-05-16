package de.felsernet.android.eiskalt

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ItemViewModel : BaseViewModel<Item>() {

    private lateinit var itemRepository: ItemRepository
    override val repository get() = itemRepository
    override val typeName: String = "item"

    private var groupMap: Map<String, Group> = emptyMap()

    @Deprecated("Use overloaded initialize instead")
    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        throw UnsupportedOperationException("Use overloaded initialize instead")
    }

    fun initialize(sharedMessageViewModel: SharedMessageViewModel, listInfo: ListInfo) {
        super.initialize(sharedMessageViewModel)
        this.itemRepository = ItemRepository(listInfo)
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
                sharedMessageViewModel.showErrorMessage("Error loading groups: ${e.message}")
            }
        }
    }

    /**
     * Group items by their groupId
     */
    override fun rebuildDisplayList() {

        // create a map with grouped items by their groupId
        // all items with unknown groups go in one group
        val grouped = _list.value.groupBy { obj ->
            val id = obj.groupId
            if (id == null) ""
            else if (id !in groupMap) "unknown"
            else id
        }

        // Sort groups by name. No group first, then others.
        val sortedGroupEntries = grouped.toList().sortedBy { (groupId, _) ->
            if (groupId.isEmpty()) ""
            else if (groupId == "Unknown") "zzz" // push Unknown to bottom
            else groupMap[groupId]?.name?.lowercase() ?: "zzz"
        }

        // add to our list
        val newList = mutableListOf<DisplayItem<Item>>()
        for ((groupId, items) in sortedGroupEntries) {
            val group = if (groupId.isEmpty()) Group("no group")
                        else groupMap[groupId] ?: Group("unknown")
            newList.add(DisplayItem.Header(group))

            items.sortedBy { it.name.lowercase() }.forEach { item ->
                newList.add(DisplayItem.Content(item))
            }
        }
        _displayList.value = newList
    }
}
