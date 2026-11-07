package de.felsernet.android.eiskalt.household

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveHouseholdTest {

    private val home = Household("h1", "Home", "alice")
    private val cellar = Household("h2", "cellar", "bob")
    private val attic = Household("h3", "Attic", "alice")

    @Test
    fun noHousehold() {
        assertNull(ActiveHousehold.choose(emptyList(), null))
        assertNull(ActiveHousehold.choose(emptyList(), "h1"))
    }

    @Test
    fun onlyOneHousehold() {
        assertEquals(home, ActiveHousehold.choose(listOf(home), null))
    }

    @Test
    fun storedHouseholdIsUsed() {
        assertEquals(cellar, ActiveHousehold.choose(listOf(home, cellar, attic), "h2"))
    }

    @Test
    fun storedHouseholdNotAMemberAnymore() {
        // e.g. left the household or removed by the owner: the only remaining one
        assertEquals(home, ActiveHousehold.choose(listOf(home), "h2"))
    }

    @Test
    fun withoutStoredHouseholdTheFirstByNameIsUsed() {
        // the case is ignored: "Attic" < "cellar" < "Home"
        assertEquals(attic, ActiveHousehold.choose(listOf(home, cellar, attic), null))
        assertEquals(attic, ActiveHousehold.choose(listOf(home, cellar, attic), "unknown"))
    }
}
