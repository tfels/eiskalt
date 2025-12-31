package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GroupRepository {

    private val db = FirebaseFirestore.getInstance()
    private val groupsCollection = db.collection("groups")

    suspend fun saveGroup(group: Group) {
        groupsCollection.document(group.id.toString()).set(group).await()
    }

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
