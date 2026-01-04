package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GroupRepository private constructor() {
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

    private val db = FirebaseFirestore.getInstance()
    private val groupsCollection = db.collection("groups")

    // ID counter management
    private var idCounter: Long = 1

    suspend fun saveGroup(group: Group) {
        // If it's a new group (id = 0), assign a new ID directly
        if (group.id == 0L) {
            group.id = nextId()
        }
        groupsCollection.document(group.id.toString()).set(group).await()
    }

    fun initializeCounter(groups: List<Group>) {
        val maxId = groups.maxOfOrNull { it.id } ?: 0
        idCounter = maxId + 1
    }

    private fun nextId(): Long = idCounter++

    suspend fun getAllGroups(): List<Group> {
        val querySnapshot = groupsCollection.get().await()
        return querySnapshot.documents.mapNotNull { it.toObject(Group::class.java) }
    }

    /**
     * Deletes a group if it's not used by any inventory items
     * @param groupId The ID of the group to delete
     * @return Pair<Boolean, Int> where first is true if deletion was successful, second is count of items still using the group
     */
    suspend fun deleteGroup(groupId: Long): Pair<Boolean, Int> {
        // Check if group is used in any inventory items
        val inventoryRepository = InventoryRepository()
        val allListNames = inventoryRepository.getAllListNames()
        var itemsUsingGroup = 0

        // Check each inventory list for items using this group
        for (listName in allListNames) {
            val items = inventoryRepository.getList(listName)
            itemsUsingGroup += items.count { it.groupId == groupId }
        }

        // If group is being used, don't delete and return false with count
        if (itemsUsingGroup > 0) {
            return Pair(false, itemsUsingGroup)
        }

        // Group is not used, safe to delete
        groupsCollection.document(groupId.toString()).delete().await()
        return Pair(true, 0)
    }

    suspend fun updateGroup(group: Group) {
        groupsCollection.document(group.id.toString()).set(group).await()
    }

    suspend fun getGroupById(groupId: Long): Group? {
        val doc = groupsCollection.document(groupId.toString()).get().await()
        return doc.toObject(Group::class.java)
    }
}
