package de.felsernet.android.eiskalt

class GroupRepository private constructor() : BaseRepository<Group>("groups", Group::class.java) {
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
     * Get all items using the group across all lists
     * @param groupId The ID of the group
     * @return List of items using the group
     */
    suspend fun getItemsUsingGroup(groupId: String): List<Item> {
        val listInfos = ListRepository().getAll()
        val allItems = mutableListOf<Item>()

        for (listInfo in listInfos) {
            val items = ItemRepository(listInfo).getAll()
            allItems.addAll(items.filter { it.groupId == groupId })
        }
        return allItems
    }

    /**
     * Deletes a group if it's not used by any items
     * @param groupId The ID of the group to delete
     * @return Pair<Boolean, Int> where first is true if deletion was successful, second is count of items still using the group
     */
    suspend fun safeDelete(groupId: String): Pair<Boolean, Int> {
        // Check if group is used in any item
        val itemsUsingGroup = getItemsUsingGroup(groupId)
        val count = itemsUsingGroup.size

        // If group is being used, don't delete and return false with count
        if (count > 0) {
            return Pair(false, count)
        }

        // Group is not used, safe to delete
        delete(groupId)
        return Pair(true, 0)
    }
}
