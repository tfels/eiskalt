package de.felsernet.android.eiskalt.household

/**
 * A household: the lists, items and groups below households/{id} are shared by its members.
 * Parsed by hand from the document (no toObject), so no R8 keep rules are needed.
 */
data class Household(
    val id: String,
    val name: String,
    val ownerId: String
)

/**
 * Choose the household to work with:
 * - the stored one (last used on this device), if the user is still a member of it
 * - otherwise the first one by name (the only one, if the user has just one)
 * - null if the user is not a member of any household
 */
object ActiveHousehold {
    fun choose(households: List<Household>, storedId: String?): Household? {
        return households.find { it.id == storedId }
            ?: households.minByOrNull { it.name.lowercase() }
    }
}
