package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
data class InventoryList(
    val name: String = "",
    val items: List<InventoryItem> = emptyList()
) {
    constructor() : this("", emptyList())
}

class InventoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val listsCollection = db.collection("inventory_lists")

    suspend fun saveList(listName: String, items: List<InventoryItem>) {
        val listDoc = InventoryList(listName, items)
        listsCollection.document(listName).set(listDoc).await()
    }

    suspend fun getList(listName: String): List<InventoryItem> {
        val doc = listsCollection.document(listName).get().await()
        return if (doc.exists()) {
            doc.toObject(InventoryList::class.java)?.items ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun deleteItem(listName: String, item: InventoryItem) {
        val doc = listsCollection.document(listName)
        doc.update("items", FieldValue.arrayRemove(item)).await()
    }

    suspend fun getAllListNames(): List<String> {
        val querySnapshot = listsCollection.get().await()
        return querySnapshot.documents.map { it.id }
    }

    suspend fun createList(listName: String) {
        if (listName.isNotBlank()) {
            val existing = listsCollection.document(listName).get().await()
            if (!existing.exists()) {
                listsCollection.document(listName).set(InventoryList(listName, emptyList())).await()
            }
        }
    }

    suspend fun deleteList(listName: String) {
        listsCollection.document(listName).delete().await()
    }
}
