package de.felsernet.android.eiskalt.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import de.felsernet.android.eiskalt.base.BaseRepository
import kotlinx.coroutines.tasks.await

/**
 * Per-user data stored in the document users/{uid}, the same on all devices of the user:
 *
 *   users/{uid}
 *     settings:        { customTitle: "..." }
 *     updatedAt:       <server timestamp>
 *
 * The document is parsed by hand (no toObject), so no R8 keep rules are needed.
 * All writes use set() with merge, so the document is created on the first write.
 *
 * The custom title is also cached in SharedPreferences, so it can be shown immediately
 * (before the DB is loaded, or when not signed in).
 */
object UserSettingsRepository {
    private const val COLLECTION = "users"
    private const val FIELD_SETTINGS = "settings"
    private const val FIELD_CUSTOM_TITLE = "customTitle"
    private const val FIELD_UPDATED_AT = "updatedAt"

    data class UserSettings(
        val customTitle: String? = null
    )

    /**
     * Load the settings of the signed-in user.
     * Returns default settings if the document does not exist yet.
     */
    suspend fun load(): UserSettings {
        val snapshot = userDocument().get().await()
        BaseRepository.readOperations++
        return parse(snapshot)
    }

    /**
     * Save the custom title; null removes it, so the default title is used.
     * The local cache is updated immediately, the DB write may complete later (e.g. when offline).
     */
    suspend fun saveCustomTitle(title: String?) {
        cacheCustomTitle(title)

        val value: Any = title ?: FieldValue.delete()
        write(mapOf(FIELD_SETTINGS to mapOf(FIELD_CUSTOM_TITLE to value)))
    }

    /**
     * Custom title from the local cache, available without DB access
     */
    fun getCachedCustomTitle(): String? = SharedPreferencesHelper.getCustomTitle()

    /**
     * Load the custom title from the DB and update the local cache (the DB value always wins).
     */
    suspend fun refreshCustomTitle(): String? {
        val title = load().customTitle
        cacheCustomTitle(title)
        return title
    }

    private fun cacheCustomTitle(title: String?) {
        if (title != null)
            SharedPreferencesHelper.saveCustomTitle(title)
        else
            SharedPreferencesHelper.clearCustomTitle()
    }

    private suspend fun write(data: Map<String, Any>) {
        userDocument().set(data + (FIELD_UPDATED_AT to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        BaseRepository.writeOperations++
    }

    private fun userDocument(): DocumentReference {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("No user signed in")
        return FirebaseFirestore.getInstance().collection(COLLECTION).document(uid)
    }

    private fun parse(snapshot: DocumentSnapshot): UserSettings {
        if (!snapshot.exists())
            return UserSettings()

        val settings = snapshot.get(FIELD_SETTINGS) as? Map<*, *>
        return UserSettings(
            customTitle = settings?.get(FIELD_CUSTOM_TITLE) as? String
        )
    }
}
