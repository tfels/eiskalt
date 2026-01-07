package de.felsernet.android.eiskalt

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ItemRepository(private val listName: String) {

    private val db = FirebaseFirestore.getInstance()
    private val listsCollection = db.collection("inventory_lists")
    private val listDoc = listsCollection.document(listName)
    private val itemsCollection = listDoc.collection("items")

    /**
     * Get all items from the list
     */
    suspend fun getList(): List<Item> {
        val querySnapshot = itemsCollection.get().await()
        return querySnapshot.documents.mapNotNull { document ->
            document.toObject(Item::class.java)?.apply {
                id = document.id
            }
        }
    }

    /**
     * Save an individual item to the list
     */
    suspend fun saveItem(item: Item) {
        // Ensure the parent document exists
        if (!listDoc.get().await().exists()) {
            listDoc.set(emptyMap<String, Any>()).await()
        }

        if (item.id.isEmpty()) {
            item.id = itemsCollection.document().id
        }
        itemsCollection.document(item.id).set(item).await()
    }

    /**
     * Delete an individual item from the list
     */
    suspend fun deleteItem(item: Item) {
        itemsCollection.document(item.id).delete().await()
    }

    /**
     * Count items in the list without fetching them
     */
    suspend fun countItems(): Int {
        val countQuery = itemsCollection.count()
        val snapshot = countQuery.get(AggregateSource.SERVER).await()
        return snapshot.count.toInt()
    }
}
