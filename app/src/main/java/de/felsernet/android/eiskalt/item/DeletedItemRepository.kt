package de.felsernet.android.eiskalt.item

import de.felsernet.android.eiskalt.base.BaseRepository
import de.felsernet.android.eiskalt.inventory.ListInfo
import java.util.Calendar
import java.util.Date

/**
 * The recently deleted items of a list (lists/<id>/deleted), offered as templates for new items.
 * Only the newest MAX_ENTRIES of the last MAX_MONTHS are kept, each name only once.
 */
class DeletedItemRepository(listInfo: ListInfo) : BaseRepository<Item>("lists/${listInfo.id}/deleted", Item::class.java) {

    companion object {
        const val MAX_ENTRIES = 10
        const val MAX_MONTHS = 6
    }

    // written by the app itself when an item is deleted, not a change of the user
    override val showPendingWrites = false

    // entries deleted before this date are too old
    private fun oldestAllowed(): Date = Calendar.getInstance().apply { add(Calendar.MONTH, -MAX_MONTHS) }.time

    /** The recently deleted items of the last MAX_MONTHS, newest first */
    suspend fun getRecent(): List<Item> {
        val oldestAllowed = oldestAllowed()
        return getAll().filter { it.deletedAt?.after(oldestAllowed) == true }.sortedByDescending { it.deletedAt }
    }

    /**
     * Remember a deleted item (with its id) as the newest entry. Older entries with the same name,
     * entries beyond MAX_ENTRIES and those older than MAX_MONTHS are deleted.
     * Returns the remaining entries, newest first.
     */
    suspend fun add(item: Item): List<Item> {
        val deleted = item.copy(deletedAt = Date())
        save(deleted)

        // all others (including the too old ones, getRecent() would skip them)
        val others = getAll().filter { it.id != deleted.id }
        val oldestAllowed = oldestAllowed()
        val kept = others
            .filterNot { it.name.equals(deleted.name, ignoreCase = true) }
            .filter { it.deletedAt?.after(oldestAllowed) == true }
            .sortedByDescending { it.deletedAt }
            .take(MAX_ENTRIES - 1)
        (others - kept.toSet()).forEach { delete(it.id) }
        return listOf(deleted) + kept
    }
}
