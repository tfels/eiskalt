package de.felsernet.android.eiskalt.item

import de.felsernet.android.eiskalt.base.BaseRepository
import de.felsernet.android.eiskalt.inventory.ListInfo

class ItemRepository(private val listInfo: ListInfo) : BaseRepository<Item>("lists/${listInfo.id}/items", Item::class.java) {
}
