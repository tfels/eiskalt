package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ListRepository {

    private val db = FirebaseFirestore.getInstance()
    private val listsCollection = db.collection("inventory_lists")

    companion object {
        var readOperations: Int = 0
        var writeOperations: Int = 0
    }

    /**
     * Get all items from a list
     */
    suspend fun getList(listName: String): List<Item> {
        val itemRepository = ItemRepository(listName)
        return itemRepository.getAll()
    }

    /**
     * Get all list names
     */
    suspend fun getAllListNames(): List<String> {
        val querySnapshot = listsCollection.get().await()
        readOperations++
        return querySnapshot.documents.map { it.id }
    }

    /**
     * Create a new list
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
     * Delete an entire list and all its items
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
        val itemRepository = ItemRepository(listName)
        for (item in items) {
            itemRepository.save(item)
        }
    }

    /**
     * Get the count of items in a list without fetching them
     */
    suspend fun getItemCount(listName: String): Int {
        val itemRepository = ItemRepository(listName)
        return itemRepository.count()
    }
}
