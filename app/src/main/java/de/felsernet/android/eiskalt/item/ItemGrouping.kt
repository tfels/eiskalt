package de.felsernet.android.eiskalt.item

import de.felsernet.android.eiskalt.base.DisplayItem
import de.felsernet.android.eiskalt.group.Group

/**
 * Builds the grouped display list of a list's items (pure function, so it can be unit tested):
 * - a header per group, followed by its items sorted by name (ignoring case)
 * - first the items without a group, then the groups sorted by name (ignoring case),
 *   last the items whose group does not exist (anymore)
 * - folded groups show only their header
 */
object ItemGrouping {
    // ids of the headers for items without a group / with a group that does not exist
    const val NO_GROUP_ID = "no_group"
    const val UNKNOWN_GROUP_ID = "unknown"

    // Type safe grouping key for items
    private sealed class GroupKey(val order: Int) {
        data object NoGroup : GroupKey(0)
        data class ValidGroup(val id: String) : GroupKey(1)
        data object UnknownGroup : GroupKey(2)
    }

    fun buildDisplayList(
        items: List<Item>,
        groups: Map<String, Group>,
        collapsedGroupIds: Set<String>
    ): List<DisplayItem<Item>> {

        // create a map with grouped items by their groupId using type safe keys
        // all items with unknown groups go in one group
        val grouped = items.groupBy { item ->
            when (val id = item.groupId) {
                null       -> GroupKey.NoGroup
                !in groups -> GroupKey.UnknownGroup
                else       -> GroupKey.ValidGroup(id)
            }
        }

        // sort the groups properly: first NoGroup, then named groups sorted by name, then UnknownGroup last
        val sortedGroupEntries = grouped.toList().sortedWith(
            compareBy({ (key, _) -> key.order },
                      { (key, _) -> if (key is GroupKey.ValidGroup) groups[key.id]?.name?.lowercase() else "" })
        )

        val displayList = mutableListOf<DisplayItem<Item>>()
        for ((groupKey, groupItems) in sortedGroupEntries) {
            val group = when (groupKey) {
                GroupKey.NoGroup       -> Group("no group", NO_GROUP_ID)
                GroupKey.UnknownGroup  -> Group("unknown", UNKNOWN_GROUP_ID)
                is GroupKey.ValidGroup -> groups.getValue(groupKey.id)
            }
            val isCollapsed = collapsedGroupIds.contains(group.id)
            displayList.add(DisplayItem.Header(group, isCollapsed))

            if (!isCollapsed) {
                groupItems.sortedBy { it.name.lowercase() }.forEach { item ->
                    displayList.add(DisplayItem.Content(item))
                }
            }
        }
        return displayList
    }
}
