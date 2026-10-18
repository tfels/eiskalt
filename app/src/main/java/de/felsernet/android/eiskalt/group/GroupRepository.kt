package de.felsernet.android.eiskalt.group

import de.felsernet.android.eiskalt.base.BaseRepository
import de.felsernet.android.eiskalt.inventory.ListInfo
import de.felsernet.android.eiskalt.inventory.ListRepository
import de.felsernet.android.eiskalt.item.Item
import de.felsernet.android.eiskalt.item.ItemRepository

class GroupRepository private constructor() : BaseRepository<Group>("groups", Group::class.java) {
    /** An item using a group, together with the list it belongs to */
    data class ItemUsage(val item: Item, val listInfo: ListInfo)

    companion object {
        // create a singleton object, so our counter will work
        @Volatile
        private var instance: GroupRepository? = null

        fun getInstance(): GroupRepository {
            return instance ?: synchronized(this) {
                instance ?: GroupRepository().also { instance = it }
            }
        }
    }

    /**
     * Get all items (of all lists) using the given group.
     * Reads all lists and their items, so this costs one read per list plus one for the lists.
     */
    suspend fun getItemsUsingGroup(groupId: String): List<ItemUsage> {
        val usages = mutableListOf<ItemUsage>()
        for (listInfo in ListRepository().getAll()) {
            ItemRepository(listInfo).getAll()
                .filter { it.groupId == groupId }
                .mapTo(usages) { ItemUsage(it, listInfo) }
        }
        return usages
    }

    /**
     * Deletes a group if it's not used by any items
     * @param groupId The ID of the group to delete
     * @return Pair<Boolean, Int> where first is true if deletion was successful, second is count of items still using the group
     */
    suspend fun safeDelete(groupId: String): Pair<Boolean, Int> {
        // Check if group is used in any item
        val itemsUsingGroup = getItemsUsingGroup(groupId).size

        // If group is being used, don't delete and return false with count
        if (itemsUsingGroup > 0) {
            return Pair(false, itemsUsingGroup)
        }

        // Group is not used, safe to delete
        delete(groupId)
        return Pair(true, 0)
    }
}
