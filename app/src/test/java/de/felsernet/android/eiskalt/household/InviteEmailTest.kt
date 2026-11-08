package de.felsernet.android.eiskalt.household

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class InviteEmailTest {

    @Test
    fun validAddressIsKept() {
        assertEquals("erin@example.com", InviteEmail.normalize("erin@example.com"))
        assertEquals("erin.m+freezer@mail.example.org", InviteEmail.normalize("erin.m+freezer@mail.example.org"))
    }

    @Test
    fun addressIsTrimmedAndLowercase() {
        // the rules only accept lowercase addresses, the token e-mail is compared in lowercase
        assertEquals("erin@example.com", InviteEmail.normalize("  Erin@Example.COM \n"))
    }

    @Test
    fun invalidAddressIsRejected() {
        assertNull(InviteEmail.normalize(""))
        assertNull(InviteEmail.normalize("   "))
        assertNull(InviteEmail.normalize("erin"))
        assertNull(InviteEmail.normalize("erin@example"))
        assertNull(InviteEmail.normalize("@example.com"))
        assertNull(InviteEmail.normalize("erin@@example.com"))
        assertNull(InviteEmail.normalize("erin smith@example.com"))
    }

    @Test
    fun inviteExpiry() {
        val now = Date(1_000_000)
        assertFalse(Invite("i1", "h1", "erin@example.com", Date(2_000_000)).isExpired(now))
        assertTrue(Invite("i1", "h1", "erin@example.com", Date(1_000_000)).isExpired(now))
        assertTrue(Invite("i1", "h1", "erin@example.com", Date(500_000)).isExpired(now))
    }
}
