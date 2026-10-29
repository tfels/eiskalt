package de.felsernet.android.eiskalt.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FirestorePathTest {

    @Test
    fun topLevelCollection() {
        assertEquals("lists", FirestorePath.collectionPath("lists"))
    }

    @Test
    fun subCollection() {
        assertEquals("lists/l1/items", FirestorePath.collectionPath("lists/l1/items"))
    }

    @Test
    fun householdPaths() {
        assertEquals("households/h1/lists", FirestorePath.collectionPath("households/h1/lists"))
        assertEquals("households/h1/lists/l1/items", FirestorePath.collectionPath("households/h1/lists/l1/items"))
        assertEquals("households/h1/groups", FirestorePath.collectionPath("households/h1/groups"))
    }

    @Test
    fun leadingAndTrailingSlashesAreRemoved() {
        assertEquals("lists/l1/items", FirestorePath.collectionPath("/lists/l1/items/"))
    }

    @Test
    fun pathEndingWithDocumentIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { FirestorePath.collectionPath("lists/l1/items/i1") }
        assertThrows(IllegalArgumentException::class.java) { FirestorePath.collectionPath("lists/l1") }
    }

    @Test
    fun emptySegmentIsRejected() {
        // e.g. a list without id: "lists/${listInfo.id}/items" with an empty id
        assertThrows(IllegalArgumentException::class.java) { FirestorePath.collectionPath("lists//items") }
        assertThrows(IllegalArgumentException::class.java) { FirestorePath.collectionPath("lists/ /items") }
    }

    @Test
    fun emptyPathIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { FirestorePath.collectionPath("") }
        assertThrows(IllegalArgumentException::class.java) { FirestorePath.collectionPath("/") }
    }
}
