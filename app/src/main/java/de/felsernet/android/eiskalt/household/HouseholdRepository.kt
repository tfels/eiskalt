package de.felsernet.android.eiskalt.household

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.felsernet.android.eiskalt.base.BaseRepository
import de.felsernet.android.eiskalt.settings.UserSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

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
    private const val COLLECTION_MEMBERS = "members"
    private const val FIELD_UID = "uid"
    private const val FIELD_NAME = "name"
    private const val FIELD_OWNER_ID = "ownerId"

    // the active household, can be observed (e.g. by the screens waiting for it)
    private val _state = MutableStateFlow<HouseholdState>(HouseholdState.Loading)
    val state: StateFlow<HouseholdState> = _state.asStateFlow()

    /** The household the app works with, null if not loaded (yet) or without household */
    val activeHousehold: Household?
        get() = (_state.value as? HouseholdState.Active)?.household

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
     * which is remembered locally. Returns null if the user is not a member of any household.
     * Updates the state; on failure the state is Error and the exception is thrown.
     */
    suspend fun loadActiveHousehold(): Household? {
        _state.value = HouseholdState.Loading
        try {
            val household = ActiveHousehold.choose(getMyHouseholds(), UserSettingsRepository.getActiveHouseholdId())
            UserSettingsRepository.saveActiveHouseholdId(household?.id)
            _state.value = if (household != null) HouseholdState.Active(household) else HouseholdState.None
            return household
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = HouseholdState.Error(e)
            throw e
        }
    }

    private fun parse(snapshot: DocumentSnapshot) = Household(
        id = snapshot.id,
        name = snapshot.getString(FIELD_NAME) ?: "",
        ownerId = snapshot.getString(FIELD_OWNER_ID) ?: ""
    )
}
