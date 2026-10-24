package de.felsernet.android.eiskalt.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import de.felsernet.android.eiskalt.base.BaseRepository
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
 * Callers don't need to care about the DB: writes run in the background, errors are logged.
 */
object UserSettingsRepository {
    private const val TAG = "settings"
    private const val COLLECTION = "users"
    private const val FIELD_SETTINGS = "settings"
    private const val FIELD_CUSTOM_TITLE = "customTitle"
    private const val FIELD_UPDATED_AT = "updatedAt"

    // Writes are not bound to a screen: Firestore completes a write only when the server
    // confirms it, which can take long when offline, and closing a screen must not cancel it.
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    data class UserSettings(
        val customTitle: String? = null
    )

    /**
     * Save the custom title; null removes it, so the default title is used.
     * The local cache is updated immediately, the DB is written in the background.
     */
    fun saveCustomTitle(title: String?) {
        cacheCustomTitle(title)

        val value: Any = title ?: FieldValue.delete()
        writeInBackground(mapOf(FIELD_SETTINGS to mapOf(FIELD_CUSTOM_TITLE to value)), "custom title")
    }

    /**
     * Custom title from the local cache, available without DB access
     */
    fun getCachedCustomTitle(): String? = SharedPreferencesHelper.getCustomTitle()

    /**
     * Load the custom title from the DB and update the local cache (the DB value always wins).
     * Returns the cached title if no user is signed in or the DB cannot be read.
     */
    suspend fun refreshCustomTitle(): String? {
        if (!isSignedIn())
            return getCachedCustomTitle()

        return try {
            val title = load().customTitle
            cacheCustomTitle(title)
            title
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to load the custom title, using the cached one", e)
            getCachedCustomTitle()
        }
    }

    private fun cacheCustomTitle(title: String?) {
        if (title != null)
            SharedPreferencesHelper.saveCustomTitle(title)
        else
            SharedPreferencesHelper.clearCustomTitle()
    }

    /**
     * Load the settings of the signed-in user.
     * Returns default settings if the document does not exist yet.
     */
    private suspend fun load(): UserSettings {
        val snapshot = userDocument().get().await()
        BaseRepository.readOperations++
        return parse(snapshot)
    }

    /**
     * Write to the DB without waiting for the result; skipped if no user is signed in
     */
    private fun writeInBackground(data: Map<String, Any>, description: String) {
        if (!isSignedIn())
            return

        writeScope.launch {
            try {
                userDocument().set(data + (FIELD_UPDATED_AT to FieldValue.serverTimestamp()), SetOptions.merge()).await()
                BaseRepository.writeOperations++
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to save the $description", e)
            }
        }
    }

    private fun isSignedIn() = FirebaseAuth.getInstance().currentUser != null

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
