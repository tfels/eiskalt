package de.felsernet.android.eiskalt

import kotlinx.coroutines.tasks.await

class ListRepository : BaseRepository<String>("lists", String::class.java) {

    // Abstract methods implementation (not used for list names, but required by base class)
    override fun getObjectId(obj: String): String {
        return obj
    }

    override fun setObjectId(obj: String, id: String): String {
        return obj
    }

    /**
     * Get all list names
     */
    override suspend fun getAll(): List<String> {
        val querySnapshot = collectionRef.get().await()
        readOperations++
        return querySnapshot.documents.map { it.id }
    }

    /**
     * Get all lists with their item counts as ListInfo objects
     */
    suspend fun getAllListInfo(): List<ListInfo> {
        val listNames = getAll()
        val listInfoList = mutableListOf<ListInfo>()

        for (listName in listNames) {
            val itemCount = ItemRepository(listName).count()
            listInfoList.add(ListInfo(listName, "", itemCount))
        }

        return listInfoList
    }

    /**
     * Create a new list
     */
    override suspend fun save(listName: String) {
        if (listName.isNotBlank()) {
            val existing = collectionRef.document(listName).get().await()
            readOperations++
            if (!existing.exists()) {
                // Document ID is the list name, no need to store name as attribute
                collectionRef.document(listName).set(emptyMap<String, Any>()).await()
                writeOperations++
            }
        }
    }

    /**
     * Delete an entire list and all its items
     */
    override suspend fun delete(listName: String) {
        // First delete all items in the subcollection
        val itemsCollection = collectionRef.document(listName).collection("items")
        val querySnapshot = itemsCollection.get().await()
        for (document in querySnapshot.documents) {
            document.reference.delete().await()
        }

        // Then delete the list document itself
        super.delete(listName)
    }
}
