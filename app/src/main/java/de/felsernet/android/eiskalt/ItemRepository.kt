package de.felsernet.android.eiskalt

class ItemRepository(private val listInfo: ListInfo) : BaseRepository<Item>("lists/${listInfo.id}/items", Item::class.java) {


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
