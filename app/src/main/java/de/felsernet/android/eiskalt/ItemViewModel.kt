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

    // Type safe grouping key for items
    sealed class GroupKey {
        data object NoGroup : GroupKey()
        data object UnknownGroup : GroupKey()
        data class ValidGroup(val id: String) : GroupKey()
    }

    override fun rebuildDisplayList() {

        // create a map with grouped items by their groupId using type safe keys
        // all items with unknown groups go in one group
        val grouped = _list.value.groupBy { obj ->
            when (val id = obj.groupId) {
                null         -> GroupKey.NoGroup
                !in groupMap -> GroupKey.UnknownGroup
                else         -> GroupKey.ValidGroup(id)
            }
        }

        // Sort the groups properly: first NoGroup, then named groups sorted by name, then UnknownGroup last
        val sortedGroupEntries = grouped.toList().sortedBy { (key, _) ->
            when (key) {
                GroupKey.NoGroup -> ""          // first
                GroupKey.UnknownGroup -> "zzz"  // last
                is GroupKey.ValidGroup -> groupMap[key.id]?.name?.lowercase() ?: "zzz"
            }
        }

        // add to our list
        val newList = mutableListOf<DisplayItem<Item>>()

        for ((groupKey, items) in sortedGroupEntries) {
            val group = when(groupKey) {
                GroupKey.NoGroup -> Group("no group")
                GroupKey.UnknownGroup -> Group("unknown")
                is GroupKey.ValidGroup -> groupMap[groupKey.id]!!
            }
            newList.add(DisplayItem.Header(group))

            items.sortedBy { it.name.lowercase() }.forEach { item ->
                newList.add(DisplayItem.Content(item))
            }
        }
        _displayList.value = newList
    }
}
