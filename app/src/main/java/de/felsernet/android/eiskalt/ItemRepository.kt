package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ItemRepository(private val listName: String) : BaseRepository<Item>("inventory_lists/$listName/items", Item::class.java) {


    /**
     * Get the ID of an Item object
     */
    override fun getObjectId(obj: Item): String {
        return obj.id
    }

    /**
     * Set the ID of an Item object
     */
    override fun setObjectId(obj: Item, id: String): Item {
        return obj.copy(id = id)
    }
}
