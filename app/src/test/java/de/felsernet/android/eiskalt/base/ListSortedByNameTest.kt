package de.felsernet.android.eiskalt.base

import de.felsernet.android.eiskalt.group.Group
import org.junit.Assert.assertEquals
import org.junit.Test

class ListSortedByNameTest {

    private fun g(name: String, id: String = "id-$name") = Group(name, id)

    private val apple = g("apple")
    private val banana = g("Banana")
    private val cherry = g("cherry")
    private val sorted = listOf(apple, banana, cherry)

    private fun List<Group>.names() = map { it.name }

    @Test
    fun sortedByNameIgnoresCase() {
        assertEquals(listOf("apple", "Banana", "cherry"), listOf(cherry, banana, apple).sortedByName().names())
    }

    @Test
    fun sortedIndexOf() {
        assertEquals(0, sorted.sortedIndexOf(g("Aaa")))
        assertEquals(1, sorted.sortedIndexOf(g("b")))
        assertEquals(2, sorted.sortedIndexOf(g("BZ")))
        assertEquals(3, sorted.sortedIndexOf(g("zebra")))
        assertEquals(0, emptyList<Group>().sortedIndexOf(g("x")))
    }

    @Test
    fun insertSortedAtStartMiddleEndAndIntoEmptyList() {
        assertEquals(listOf("Aaa", "apple", "Banana", "cherry"), sorted.insertSorted(g("Aaa")).names())
        assertEquals(listOf("apple", "Banana", "BZ", "cherry"), sorted.insertSorted(g("BZ")).names())
        assertEquals(listOf("apple", "Banana", "cherry", "zebra"), sorted.insertSorted(g("zebra")).names())
        assertEquals(listOf("x"), emptyList<Group>().insertSorted(g("x")).names())
    }

    @Test
    fun insertSortedWithSameNameKeepsBoth() {
        val result = sorted.insertSorted(g("BANANA", "other-id"))
        assertEquals(4, result.size)
        assertEquals(listOf("apple", "banana", "banana", "cherry"), result.names().map { it.lowercase() })
    }

    @Test
    fun replaceSortedMovesRenamedEntry() {
        val renamed = g("zebra", apple.id)
        assertEquals(listOf("Banana", "cherry", "zebra"), sorted.replaceSorted(renamed).names())
    }

    @Test
    fun replaceSortedWithSameNameUpdatesEntry() {
        val changed = Group(banana.name, banana.id, comment = "yellow")
        val result = sorted.replaceSorted(changed)
        assertEquals(listOf(apple, changed, cherry), result)
    }

    @Test
    fun replaceSortedOfUnknownIdInserts() {
        assertEquals(listOf("apple", "Banana", "BZ", "cherry"), sorted.replaceSorted(g("BZ")).names())
    }

    @Test
    fun deleteById() {
        // the name does not matter, only the id
        assertEquals(listOf(apple, cherry), sorted.deleteById(g("renamed", banana.id)))
    }

    @Test
    fun deleteByIdOfUnknownIdChangesNothing() {
        assertEquals(sorted, sorted.deleteById(g("unknown")))
    }

    @Test
    fun originalListIsNotChanged() {
        val original = sorted.toList()
        sorted.insertSorted(g("x"))
        sorted.replaceSorted(g("zebra", apple.id))
        sorted.deleteById(banana)
        assertEquals(original, sorted)
    }
}
