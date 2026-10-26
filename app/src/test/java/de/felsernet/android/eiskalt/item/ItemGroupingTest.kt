package de.felsernet.android.eiskalt.item

import de.felsernet.android.eiskalt.base.DisplayItem
import de.felsernet.android.eiskalt.group.Group
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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

    private fun build(items: List<Item>, collapsed: Set<String> = emptySet()) =
        ItemGrouping.buildDisplayList(items, groups, collapsed).describe()

    @Test
    fun noItems() {
        assertTrue(ItemGrouping.buildDisplayList(emptyList(), groups, emptySet()).isEmpty())
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
}
