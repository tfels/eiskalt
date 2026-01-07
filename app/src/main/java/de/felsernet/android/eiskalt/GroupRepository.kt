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

    suspend fun save(group: Group) {
        // If it's a new group (id is empty), generate a new Firestore document ID
        if (group.id.isEmpty()) {
            group.id = groupsCollection.document().id
        }
        groupsCollection.document(group.id).set(group).await()
    }

    suspend fun getAll(): List<Group> {
        val querySnapshot = groupsCollection.get().await()
        return querySnapshot.documents.mapNotNull { it.toObject(Group::class.java) }
    }

    /**
     * Deletes a group if it's not used by any items
     * @param groupId The ID of the group to delete
     * @return Pair<Boolean, Int> where first is true if deletion was successful, second is count of items still using the group
     */
    suspend fun delete(groupId: String): Pair<Boolean, Int> {
        // Check if group is used in any item
        val listRepository = ListRepository()
        val allListNames = listRepository.getAll()
        var itemsUsingGroup = 0

        // Check each list for items using this group
        for (listName in allListNames) {
            val items = listRepository.getList(listName)
            itemsUsingGroup += items.count { it.groupId == groupId }
        }

        // If group is being used, don't delete and return false with count
        if (itemsUsingGroup > 0) {
            return Pair(false, itemsUsingGroup)
        }

        // Group is not used, safe to delete
        groupsCollection.document(groupId).delete().await()
        return Pair(true, 0)
    }

    suspend fun update(group: Group) {
        groupsCollection.document(group.id).set(group).await()
    }

    suspend fun getById(groupId: String): Group? {
        val doc = groupsCollection.document(groupId).get().await()
        return doc.toObject(Group::class.java)
    }
}
