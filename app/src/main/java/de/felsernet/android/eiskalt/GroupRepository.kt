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

    suspend fun deleteGroup(groupId: Long) {
        groupsCollection.document(groupId.toString()).delete().await()
    }

    suspend fun renameGroup(groupId: Long, newName: String) {
        groupsCollection.document(groupId.toString()).update("name", newName).await()
    }

    suspend fun getGroupById(groupId: Long): Group? {
        val doc = groupsCollection.document(groupId.toString()).get().await()
        return doc.toObject(Group::class.java)
    }
}
