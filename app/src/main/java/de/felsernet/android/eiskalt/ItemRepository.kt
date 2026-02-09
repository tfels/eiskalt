package de.felsernet.android.eiskalt

class ItemRepository(private val listInfo: ListInfo) : BaseRepository<Item>("lists/${listInfo.id}/items", Item::class.java) {
}
