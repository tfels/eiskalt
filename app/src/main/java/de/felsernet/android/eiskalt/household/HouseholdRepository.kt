package de.felsernet.android.eiskalt.household

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.felsernet.android.eiskalt.base.BaseRepository
import de.felsernet.android.eiskalt.log.AppLog
import de.felsernet.android.eiskalt.settings.UserSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * The households of the signed-in user and the active one (the one the app works with).
 *
 *   households/{hid}                 name, createdBy, ownerId
 *   households/{hid}/members/{uid}   uid, name
 *
 * The households are found with a collection group query on all memberships with uid == own uid
 * (needs the collection group index on members.uid, see firebase/firestore.indexes.json).
 * The active household is only stored locally, so each device can use another one.
 */
object HouseholdRepository {
    private const val TAG = "household"
    private const val COLLECTION_HOUSEHOLDS = "households"
    private const val COLLECTION_MEMBERS = "members"
    private const val COLLECTION_INVITES = "invites"
    private const val FIELD_HOUSEHOLD_ID = "householdId"
    private const val FIELD_EMAIL = "email"
    private const val FIELD_INVITED_BY = "invitedBy"
    private const val FIELD_INVITED_BY_NAME = "invitedByName"
    private const val FIELD_HOUSEHOLD_NAME = "householdName"
    private const val FIELD_EXPIRES_AT = "expiresAt"

    // The rules allow at most 7 days, checked against the server time:
    // one day less, so the invite is not rejected if the clock of the device is ahead
    private const val INVITE_VALID_DAYS = 6L
    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    private const val FIELD_UID = "uid"
    private const val FIELD_NAME = "name"
    private const val FIELD_OWNER_ID = "ownerId"

    // the active household, can be observed (e.g. by the screens waiting for it)
    private val _state = MutableStateFlow<HouseholdState>(HouseholdState.Loading)
    val state: StateFlow<HouseholdState> = _state.asStateFlow()

    /** The household the app works with, null if not loaded (yet) or without household */
    val activeHousehold: Household?
        get() = (_state.value as? HouseholdState.Active)?.household

    /** All households of the user from the last load, sorted by name (e.g. to switch between them) */
    var households: List<Household> = emptyList()
        private set

    /**
     * Load the households of the signed-in user, sorted by name
     */
    suspend fun getMyHouseholds(): List<Household> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("No user signed in")

        val memberships = FirebaseFirestore.getInstance()
            .collectionGroup(COLLECTION_MEMBERS)
            .whereEqualTo(FIELD_UID, uid)
            .get().await()
        BaseRepository.readOperations++

        // the membership households/{hid}/members/{uid} has the household as grandparent
        return memberships.documents
            .mapNotNull { it.reference.parent.parent }
            .map { householdRef ->
                val snapshot = householdRef.get().await()
                BaseRepository.readOperations++
                parse(snapshot)
            }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Load the households of the signed-in user and choose the active one (see ActiveHousehold),
     * which is remembered locally. The result is provided as state (Active, None, Choose or Error),
     * a failure is logged.
     */
    suspend fun loadActiveHousehold() {
        _state.value = HouseholdState.Loading
        try {
            households = getMyHouseholds()
            val household = ActiveHousehold.choose(households, UserSettingsRepository.getActiveHouseholdId())
            UserSettingsRepository.saveActiveHouseholdId(household?.id)
            _state.value = when {
                household != null -> HouseholdState.Active(household)
                households.isEmpty() -> HouseholdState.None
                else -> HouseholdState.Choose
            }
            AppLog.i(TAG, when {
                household != null -> "Active household: \"${household.name}\""
                households.isEmpty() -> "Not a member of any household"
                else -> "Member of ${households.size} households, the user has to choose one"
            })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to load the households", e)
            _state.value = HouseholdState.Error(e)
        }
    }

    /**
     * Switch to another household of the user, remembered locally (the screens reload their data)
     */
    fun switchTo(household: Household) {
        UserSettingsRepository.saveActiveHouseholdId(household.id)
        _state.value = HouseholdState.Active(household)
        AppLog.i(TAG, "Switched to household: \"${household.name}\"")
    }

    /**
     * Load the members of a household, sorted by name
     */
    suspend fun getMembers(householdId: String): List<HouseholdMember> {
        val snapshot = householdDocument(householdId).collection(COLLECTION_MEMBERS).get().await()
        BaseRepository.readOperations++
        return snapshot.documents
            .map { HouseholdMember(uid = it.id, name = it.getString(FIELD_NAME) ?: it.id) }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Rename the active household (every member may do this).
     * The new name is also used in the state and the list of households.
     */
    suspend fun renameActiveHousehold(name: String) {
        val household = activeHousehold ?: throw IllegalStateException("No active household")
        householdDocument(household.id).update(FIELD_NAME, name).await()
        BaseRepository.writeOperations++

        val renamed = household.copy(name = name)
        households = households.map { if (it.id == renamed.id) renamed else it }.sortedBy { it.name.lowercase() }
        _state.value = HouseholdState.Active(renamed)
        AppLog.i(TAG, "Renamed the household to: \"$name\"")
    }

    /**
     * Leave the active household (not allowed for the owner, who has to hand over the ownership first).
     * Afterwards the households are loaded again: the remaining one, a choice or none (see state).
     */
    suspend fun leaveActiveHousehold() {
        val household = activeHousehold ?: throw IllegalStateException("No active household")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("No user signed in")

        householdDocument(household.id).collection(COLLECTION_MEMBERS).document(uid).delete().await()
        BaseRepository.writeOperations++
        AppLog.i(TAG, "Left the household: \"${household.name}\"")
        loadActiveHousehold()
    }

    /**
     * Remove another member from the active household (only the owner may do this)
     */
    suspend fun removeMember(member: HouseholdMember) {
        val household = activeHousehold ?: throw IllegalStateException("No active household")
        householdDocument(household.id).collection(COLLECTION_MEMBERS).document(member.uid).delete().await()
        BaseRepository.writeOperations++
        AppLog.i(TAG, "Removed \"${member.name}\" from the household \"${household.name}\"")
    }

    /**
     * Make another member the owner of the active household (only the owner may do this).
     * The new owner is also used in the state and the list of households.
     */
    suspend fun handOverOwnership(member: HouseholdMember) {
        val household = activeHousehold ?: throw IllegalStateException("No active household")
        householdDocument(household.id).update(FIELD_OWNER_ID, member.uid).await()
        BaseRepository.writeOperations++

        val handedOver = household.copy(ownerId = member.uid)
        households = households.map { if (it.id == handedOver.id) handedOver else it }
        _state.value = HouseholdState.Active(handedOver)
        AppLog.i(TAG, "Made \"${member.name}\" the owner of the household \"${household.name}\"")
    }

    /**
     * Load the invites to a household (open and expired ones), sorted by e-mail address
     */
    suspend fun getInvites(householdId: String): List<Invite> {
        val snapshot = FirebaseFirestore.getInstance().collection(COLLECTION_INVITES)
            .whereEqualTo(FIELD_HOUSEHOLD_ID, householdId)
            .get().await()
        BaseRepository.readOperations++
        return snapshot.documents
            .mapNotNull { doc ->
                val email = doc.getString(FIELD_EMAIL) ?: return@mapNotNull null
                val expiresAt = doc.getTimestamp(FIELD_EXPIRES_AT)?.toDate() ?: return@mapNotNull null
                Invite(doc.id, householdId, email, expiresAt)
            }
            .sortedBy { it.email }
    }

    /**
     * Invite the user with the e-mail address (checked and normalized, see InviteEmail)
     * to the active household, valid for INVITE_VALID_DAYS.
     * The invited user can't read the household or the inviting user, so their names are
     * part of the invite (the rules check that the household name is the current one).
     * @return the new invite object (e.g. to share it)
     */
    suspend fun createInvite(email: String): Invite {
        val household = activeHousehold ?: throw IllegalStateException("No active household")
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("No user signed in")

        val expiresAt = Date(System.currentTimeMillis() + INVITE_VALID_DAYS * DAY_MILLIS)
        val document = FirebaseFirestore.getInstance().collection(COLLECTION_INVITES).add(mapOf(
            FIELD_HOUSEHOLD_ID to household.id,
            FIELD_HOUSEHOLD_NAME to household.name,
            FIELD_EMAIL to email,
            FIELD_INVITED_BY to user.uid,
            FIELD_INVITED_BY_NAME to displayName(user),
            FIELD_EXPIRES_AT to expiresAt
        )).await()
        BaseRepository.writeOperations++
        AppLog.i(TAG, "Invited \"$email\" to the household \"${household.name}\"")
        return Invite(document.id, household.id, email, expiresAt)
    }

    /**
     * Revoke (delete) an invite
     */
    suspend fun revokeInvite(invite: Invite) {
        FirebaseFirestore.getInstance().collection(COLLECTION_INVITES).document(invite.id).delete().await()
        BaseRepository.writeOperations++
        AppLog.i(TAG, "Revoked the invite for \"${invite.email}\"")
    }

    /**
     * Forget the active household, e.g. after signing out (another user may sign in)
     */
    fun reset() {
        households = emptyList()
        _state.value = HouseholdState.Loading
    }

    // the name of the user shown to others (e.g. in invites): the name of the Google account,
    // or the part of the e-mail address before the @ (at most 100 characters, as in the rules)
    private fun displayName(user: FirebaseUser): String {
        val name = user.displayName?.trim().orEmpty()
            .ifEmpty { user.email?.substringBefore('@').orEmpty() }
            .ifEmpty { user.uid }
        return name.take(100)
    }

    private fun householdDocument(householdId: String) =
        FirebaseFirestore.getInstance().collection(COLLECTION_HOUSEHOLDS).document(householdId)

    private fun parse(snapshot: DocumentSnapshot) = Household(
        id = snapshot.id,
        name = snapshot.getString(FIELD_NAME) ?: "",
        ownerId = snapshot.getString(FIELD_OWNER_ID) ?: ""
    )
}
