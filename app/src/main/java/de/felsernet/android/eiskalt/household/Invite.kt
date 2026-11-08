package de.felsernet.android.eiskalt.household

import java.util.Date

/**
 * An invite to a household (invites/{id}): the user with this (verified) e-mail address
 * can join the household until the invite expires. Used up (deleted) when joining.
 */
data class Invite(
    val id: String,
    val householdId: String,
    val email: String,
    val expiresAt: Date
) {
    fun isExpired(now: Date = Date()) = !expiresAt.after(now)
}

/**
 * Check of the e-mail address entered for an invite (no Android dependency, so it can be unit tested)
 */
object InviteEmail {
    // simple check of the form, as in the rules (they only allow lowercase addresses):
    // something @ something . something, without spaces and without a second @
    private val EMAIL = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

    /**
     * The address as stored in the invite (trimmed, lowercase), or null if it is not valid
     */
    fun normalize(input: String): String? {
        val email = input.trim().lowercase()
        return email.takeIf { EMAIL.matches(it) }
    }
}
