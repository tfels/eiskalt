package de.felsernet.android.eiskalt.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class PendingWritesTest {

    private fun entry(name: String, failed: Boolean = false) =
        PendingWrites.Entry(7, PendingWrites.Operation.UPDATE, name, Date(1_700_000_000_000L), failed)

    @Test
    fun entryIsRestoredUnchanged() {
        val entry = entry("Milk")
        assertEquals(entry, PendingWrites.decode(PendingWrites.encode(entry)))
    }

    @Test
    fun failedFlagIsRestored() {
        val entry = entry("Milk", failed = true)
        assertEquals(entry, PendingWrites.decode(PendingWrites.encode(entry)))
    }

    @Test
    fun nameMayContainTheSeparator() {
        val entry = entry("Milk | 1,5 %")
        assertEquals(entry, PendingWrites.decode(PendingWrites.encode(entry)))
    }

    @Test
    fun emptyNameIsRestored() {
        val entry = entry("")
        assertEquals(entry, PendingWrites.decode(PendingWrites.encode(entry)))
    }

    @Test
    fun unreadableEntriesAreSkipped() {
        assertNull(PendingWrites.decode(""))
        assertNull(PendingWrites.decode("7|UPDATE|123"))
        assertNull(PendingWrites.decode("x|UPDATE|123|false|Milk"))
        assertNull(PendingWrites.decode("7|UNKNOWN|123|false|Milk"))
        assertNull(PendingWrites.decode("7|UPDATE|x|false|Milk"))
        assertNull(PendingWrites.decode("7|UPDATE|123|maybe|Milk"))
    }
}
