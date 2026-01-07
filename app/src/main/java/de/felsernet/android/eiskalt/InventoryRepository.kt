package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class InventoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val listsCollection = db.collection("inventory_lists")

    companion object {
        var readOperations: Int = 0
        var writeOperations: Int = 0
    }

    /**
     * Save an individual item to the list
     */
    suspend fun saveItem(listName: String, item: Item) {
        // Ensure the parent document exists
        createList(listName)

        val itemsCollection = listsCollection.document(listName).collection("items")
        if (item.id.isEmpty()) {
            item.id = itemsCollection.document().id
        }
        itemsCollection.document(item.id).set(item).await()
        writeOperations++
    }

    /**
     * Get all items from a list
     */
    suspend fun getList(listName: String): List<Item> {
        val itemsCollection = listsCollection.document(listName).collection("items")
        val querySnapshot = itemsCollection.get().await()
        readOperations++
        return querySnapshot.documents.mapNotNull { document ->
            document.toObject(Item::class.java)?.apply {
                id = document.id
            }
        }
    }

    /**
     * Delete an individual item from the list
     */
    suspend fun deleteItem(listName: String, item: Item) {
        val itemsCollection = listsCollection.document(listName).collection("items")
        itemsCollection.document(item.id.toString()).delete().await()
        writeOperations++
    }

    /**
     * Get all inventory list names
     */
    suspend fun getAllListNames(): List<String> {
        val querySnapshot = listsCollection.get().await()
        readOperations++
        return querySnapshot.documents.map { it.id }
    }

    /**
     * Create a new inventory list
     */
    suspend fun createList(listName: String) {
        if (listName.isNotBlank()) {
            val existing = listsCollection.document(listName).get().await()
            readOperations++
            if (!existing.exists()) {
                // Document ID is the list name, no need to store name as attribute
                listsCollection.document(listName).set(emptyMap<String, Any>()).await()
                writeOperations++
            }
        }
    }

    /**
     * Delete an entire inventory list and all its items
     */
    suspend fun deleteList(listName: String) {
        // First delete all items in the subcollection
        val itemsCollection = listsCollection.document(listName).collection("items")
        val querySnapshot = itemsCollection.get().await()
        for (document in querySnapshot.documents) {
            document.reference.delete().await()
        }

        // Then delete the list document itself
        listsCollection.document(listName).delete().await()
        writeOperations++
    }

    /**
     * Save multiple items at once (for migration or bulk operations)
     */
    suspend fun saveList(listName: String, items: List<Item>) {
        // Ensure the parent document exists
        createList(listName)
        for (item in items) {
            saveItem(listName, item)
        }
    }
}
