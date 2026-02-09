package de.felsernet.android.eiskalt

import kotlinx.coroutines.tasks.await

class ListRepository : BaseRepository<ListInfo>("lists", ListInfo::class.java) {

    /**
     * Get all lists with their item counts as ListInfo objects
     */
    override suspend fun getAll(): List<ListInfo> {
        var listInfoList = super.getAll()

        for (listInfo in listInfoList) {
            val itemCount = ItemRepository(listInfo).count()
            listInfo.itemCount = itemCount
        }

        return listInfoList
    }

    /**
     * Delete all its items before deleting the list itself
     */
    override suspend fun delete(id: String) {
        // First delete all items in the subcollection
        val itemsCollection = collectionRef.document(id).collection("items")
        val querySnapshot = itemsCollection.get().await()
        for (document in querySnapshot.documents) {
            document.reference.delete().await()
        }

        // Then delete the list document itself
        super.delete(id)
    }
}
