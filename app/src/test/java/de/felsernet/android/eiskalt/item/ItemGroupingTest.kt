package de.felsernet.android.eiskalt.item

import de.felsernet.android.eiskalt.base.DisplayItem
import de.felsernet.android.eiskalt.group.Group
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ItemGroupingTest {

    private val drinks = Group("Drinks", "g-drinks")
    private val meat = Group("meat", "g-meat")
    private val zzz = Group("Zzz-Ice", "g-zzz")
    private val groups = listOf(drinks, meat, zzz).associateBy { it.id }

    private fun item(name: String, groupId: String? = null) = Item(name, id = "i-$name", quantity = 1, groupId = groupId)

    // short readable form of the display list: headers as "[group id]" / "[group id]+" if folded, items by name
    private fun List<DisplayItem<Item>>.describe(): List<String> = map {
        when (it) {
            is DisplayItem.Header<*> -> "[${it.data.id}]" + if (it.isCollapsed) "+" else ""
            is DisplayItem.Content -> it.obj.name
        }
    }

    // a recently deleted item (template)
    private fun deleted(name: String) = item(name).copy(id = "d-$name", deletedAt = Date())

    private fun build(items: List<Item>, collapsed: Set<String> = emptySet(), recentlyDeleted: List<Item> = emptyList()) =
        ItemGrouping.buildDisplayList(items, recentlyDeleted, groups, collapsed, "No Group", "Unknown Group", "Recently Deleted").describe()

    @Test
    fun noItems() {
        assertTrue(ItemGrouping.buildDisplayList(emptyList(), emptyList(), groups, emptySet(), "No Group", "Unknown Group", "Recently Deleted").isEmpty())
    }

    @Test
    fun itemsWithoutGroupAreSortedByNameIgnoringCase() {
        assertEquals(
            listOf("[no_group]", "apple", "Banana", "cherry"),
            build(listOf(item("cherry"), item("Banana"), item("apple")))
        )
    }

    @Test
    fun groupOrder_noGroupFirst_groupsByNameIgnoringCase_unknownLast() {
        val items = listOf(
            item("steak", meat.id),
            item("lost", "deleted-group"),
            item("bread"),
            item("water", drinks.id),
        )
        assertEquals(
            listOf("[no_group]", "bread", "[g-drinks]", "water", "[g-meat]", "steak", "[unknown]", "lost"),
            build(items)
        )
    }

    @Test
    fun unknownGroupIsLastEvenIfAGroupNameSortsAfterZzz() {
        val items = listOf(item("lost", "deleted-group"), item("ice", zzz.id))
        assertEquals(listOf("[g-zzz]", "ice", "[unknown]", "lost"), build(items))
    }

    @Test
    fun foldedGroupShowsOnlyItsHeader() {
        val items = listOf(item("water", drinks.id), item("juice", drinks.id), item("steak", meat.id))
        assertEquals(
            listOf("[g-drinks]+", "[g-meat]", "steak"),
            build(items, collapsed = setOf(drinks.id))
        )
    }

    @Test
    fun specialHeadersCanBeFolded() {
        val items = listOf(item("bread"), item("lost", "deleted-group"))
        assertEquals(
            listOf("[no_group]+", "[unknown]+"),
            build(items, collapsed = setOf(ItemGrouping.NO_GROUP_ID, ItemGrouping.UNKNOWN_GROUP_ID))
        )
    }

    @Test
    fun groupsWithoutItemsAreNotShown() {
        assertEquals(listOf("[g-meat]", "steak"), build(listOf(item("steak", meat.id))))
    }

    @Test
    fun foldedIdsOfOtherGroupsAreIgnored() {
        // e.g. stale ids of deleted groups in the folded groups
        assertEquals(listOf("[g-meat]", "steak"), build(listOf(item("steak", meat.id)), collapsed = setOf("deleted-group")))
    }

    @Test
    fun recentlyDeletedAreShownAtTheEndInTheGivenOrder() {
        // the repository delivers them newest first, they are not sorted by name
        assertEquals(
            listOf("[no_group]", "bread", "[recently_deleted]", "salt", "apple"),
            build(listOf(item("bread")), recentlyDeleted = listOf(deleted("salt"), deleted("apple")))
        )
    }

    @Test
    fun recentlyDeletedWithANameInTheListAreNotShownTwice() {
        assertEquals(
            listOf("[g-drinks]", "Milk", "[recently_deleted]", "salt"),
            build(listOf(item("Milk", drinks.id)), recentlyDeleted = listOf(deleted("milk"), deleted("salt")))
        )
    }

    @Test
    fun usedTemplateIsRemovedAndTheEmptyHeaderToo() {
        val templates = listOf(deleted("milk"))
        assertEquals(listOf("[recently_deleted]", "milk"), build(emptyList(), recentlyDeleted = templates))

        // a new item was created from the template
        assertEquals(listOf("[no_group]", "milk"), build(listOf(item("milk")), recentlyDeleted = templates))
    }

    @Test
    fun recentlyDeletedCanBeFolded() {
        assertEquals(
            listOf("[recently_deleted]+"),
            build(emptyList(), collapsed = setOf(ItemGrouping.RECENTLY_DELETED_ID), recentlyDeleted = listOf(deleted("salt")))
        )
    }
}
