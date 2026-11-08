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
 * A member of a household (households/{hid}/members/{uid})
 */
data class HouseholdMember(
    val uid: String,
    val name: String
)

/**
 * State of the active household (see HouseholdRepository.state)
 */
sealed class HouseholdState {
    /** not loaded yet (or loading) */
    object Loading : HouseholdState()

    /** the user is not a member of any household */
    object None : HouseholdState()

    /** the user is a member of several households, but none is chosen on this device yet */
    object Choose : HouseholdState()

    data class Active(val household: Household) : HouseholdState()

    /** loading failed (logged by HouseholdRepository), e.g. offline on the first start */
    data class Error(val exception: Exception) : HouseholdState()
}

/**
 * Choose the household to work with:
 * - the stored one (last used on this device), if the user is still a member of it
 * - otherwise the only one, if the user is a member of just one household
 * - null otherwise: no household at all, or several and the user has to choose one
 */
object ActiveHousehold {
    fun choose(households: List<Household>, storedId: String?): Household? {
        return households.find { it.id == storedId }
            ?: households.singleOrNull()
    }
}
