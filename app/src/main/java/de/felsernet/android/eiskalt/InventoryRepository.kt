package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
data class InventoryList(
    val name: String = "",
    val items: List<Item> = emptyList()
) {
    constructor() : this("", emptyList())
}

class InventoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val listsCollection = db.collection("inventory_lists")

    companion object {
        var readOperations: Int = 0
        var writeOperations: Int = 0
    }

    suspend fun saveList(listName: String, items: List<Item>) {
        val listDoc = InventoryList(listName, items)
        listsCollection.document(listName).set(listDoc).await()
        writeOperations++
    }

    suspend fun getList(listName: String): List<Item> {
        val doc = listsCollection.document(listName).get().await()
        readOperations++
        return if (doc.exists()) {
            doc.toObject(InventoryList::class.java)?.items ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun deleteItem(listName: String, item: Item) {
        val doc = listsCollection.document(listName)
        doc.update("items", FieldValue.arrayRemove(item)).await()
        writeOperations++
    }

    suspend fun getAllListNames(): List<String> {
        val querySnapshot = listsCollection.get().await()
        readOperations++
        return querySnapshot.documents.map { it.id }
    }

    suspend fun createList(listName: String) {
        if (listName.isNotBlank()) {
            val existing = listsCollection.document(listName).get().await()
            readOperations++
            if (!existing.exists()) {
                listsCollection.document(listName).set(InventoryList(listName, emptyList())).await()
                writeOperations++
            }
        }
    }

    suspend fun deleteList(listName: String) {
        listsCollection.document(listName).delete().await()
        writeOperations++
    }
}
