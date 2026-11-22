package de.felsernet.android.eiskalt.inventory

import de.felsernet.android.eiskalt.base.BaseRepository
import de.felsernet.android.eiskalt.item.ItemRepository
import kotlinx.coroutines.tasks.await

class ListRepository : BaseRepository<ListInfo>("lists", ListInfo::class.java) {

    /**
     * Get all lists with their item counts as ListInfo objects
     */
    override suspend fun getAll(): List<ListInfo> {
        var listInfoList = super.getAll()

        for (listInfo in listInfoList) {
            val itemCount = ItemRepository(listInfo).countOrCached()
            listInfo.itemCount = itemCount
        }

        return listInfoList
    }

    /**
     * Delete all its items and recently deleted items before deleting the list itself
     */
    override suspend fun delete(id: String) {
        // First delete all documents in the subcollections
        for (subcollection in listOf("items", "deleted")) {
            val querySnapshot = collectionRef.document(id).collection(subcollection).get().await()
            for (document in querySnapshot.documents) {
                document.reference.delete().await()
            }
        }

        // Then delete the list document itself
        super.delete(id)
    }
}
