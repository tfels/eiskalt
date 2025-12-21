package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class InventoryList(
    val name: String = "default",
    val items: List<InventoryItem> = emptyList()
)

class InventoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val listsCollection = db.collection("inventory_lists")

    suspend fun saveList(items: List<InventoryItem>) {
        val listDoc = InventoryList("default", items)
        listsCollection.document("default").set(listDoc).await()
    }

    suspend fun getList(): List<InventoryItem> {
        val doc = listsCollection.document("default").get().await()
        return if (doc.exists()) {
            doc.toObject(InventoryList::class.java)?.items ?: emptyList()
        } else {
            emptyList()
        }
    }
}
