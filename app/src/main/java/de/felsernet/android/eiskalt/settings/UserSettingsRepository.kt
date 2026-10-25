package de.felsernet.android.eiskalt.settings

import android.content.Context
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
 *     settings:        { customTitle: "...", syncLastViewedList: true }
 *     lastViewedListId: "..."                 // the list that is currently open, removed when going back
 *                                             // (only used if syncLastViewedList is enabled)
 *     collapsedGroups: { <listId>: [<groupId>, ...] }   // folded groups per list, unfolded is the default
 *     updatedAt:       <server timestamp>
 *
 * The document is parsed by hand (no toObject), so no R8 keep rules are needed.
 * All writes use set() with merge, so the document is created on the first write.
 *
 * All values are also stored locally in SharedPreferences, so they are available immediately
 * (before the DB is loaded, when offline or when not signed in).
 * Callers don't need to care about the DB: writes run in the background, errors are logged.
 */
object UserSettingsRepository {
    private const val TAG = "settings"
    private const val COLLECTION = "users"
    private const val FIELD_SETTINGS = "settings"
    private const val FIELD_CUSTOM_TITLE = "customTitle"
    private const val FIELD_SYNC_LAST_VIEWED_LIST = "syncLastViewedList"
    private const val FIELD_LAST_VIEWED_LIST_ID = "lastViewedListId"
    private const val FIELD_COLLAPSED_GROUPS = "collapsedGroups"
    private const val FIELD_UPDATED_AT = "updatedAt"

    // Writes are not bound to a screen: Firestore completes a write only when the server
    // confirms it, which can take long when offline, and closing a screen must not cancel it.
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * The settings the user can change in the settings screen
     */
    data class UserSettings(
        val customTitle: String? = null,
        val syncLastViewedList: Boolean = false
    )

    /**
     * Content of the document users/{uid}: the settings and the state stored by the app itself
     */
    private data class UserDocument(
        val settings: UserSettings = UserSettings(),
        val lastViewedListId: String? = null,
        val collapsedGroups: Map<String, Set<String>> = emptyMap()
    )

    /**
     * Initialize the local cache, call once at app start
     */
    fun initialize(context: Context) {
        UserSettingsCache.initialize(context)
    }

    /**
     * Save the settings; a null title removes it, so the default title is used.
     * The local cache is updated immediately, the DB is written in the background.
     * Disabling syncLastViewedList also removes the last viewed list from the DB.
     */
    fun saveSettings(settings: UserSettings) {
        cacheSettings(settings)

        val title: Any = settings.customTitle ?: FieldValue.delete()
        val data = mutableMapOf<String, Any>(FIELD_SETTINGS to mapOf(
            FIELD_CUSTOM_TITLE to title,
            FIELD_SYNC_LAST_VIEWED_LIST to settings.syncLastViewedList
        ))
        if (!settings.syncLastViewedList)
            data[FIELD_LAST_VIEWED_LIST_ID] = FieldValue.delete()
        writeInBackground(data, "settings")
    }

    /**
     * Settings from the local cache, available without DB access
     */
    fun getCachedSettings() = UserSettings(
        customTitle = UserSettingsCache.getCustomTitle(),
        syncLastViewedList = UserSettingsCache.isSyncLastViewedList()
    )

    /**
     * Load the settings from the DB and update the local cache (the DB values always win).
     * Returns the cached settings if no user is signed in or the DB cannot be read.
     */
    suspend fun refreshSettings(): UserSettings {
        if (!isSignedIn())
            return getCachedSettings()

        return try {
            val settings = load().settings
            cacheSettings(settings)
            settings
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to load the settings, using the cached ones", e)
            getCachedSettings()
        }
    }

    private fun cacheSettings(settings: UserSettings) {
        if (settings.customTitle != null)
            UserSettingsCache.saveCustomTitle(settings.customTitle)
        else
            UserSettingsCache.clearCustomTitle()
        UserSettingsCache.setSyncLastViewedList(settings.syncLastViewedList)
    }

    /**
     * Save the id of the list that is currently open; null removes it (back on the list overview).
     * Stored locally immediately, the DB is written in the background if syncLastViewedList is enabled.
     */
    fun saveLastViewedList(listId: String?) {
        cacheLastViewedList(listId)
        if (!UserSettingsCache.isSyncLastViewedList())
            return

        val value: Any = listId ?: FieldValue.delete()
        writeInBackground(mapOf(FIELD_LAST_VIEWED_LIST_ID to value), "last viewed list")
    }

    /**
     * Id of the last viewed list from the local cache, available without DB access
     */
    fun getLastViewedList(): String? = UserSettingsCache.getLastViewedList()

    /**
     * Load the last viewed list from the DB (it may be opened on another device)
     * and update the local cache (the DB value always wins).
     * Returns the cached value if syncLastViewedList is disabled, no user is signed in
     * or the DB cannot be read.
     */
    suspend fun refreshLastViewedList(): String? {
        if (!UserSettingsCache.isSyncLastViewedList() || !isSignedIn())
            return getLastViewedList()

        return try {
            val listId = load().lastViewedListId
            cacheLastViewedList(listId)
            listId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to load the last viewed list, using the cached one", e)
            getLastViewedList()
        }
    }

    private fun cacheLastViewedList(listId: String?) {
        if (listId != null)
            UserSettingsCache.saveLastViewedList(listId)
        else
            UserSettingsCache.clearLastViewedList()
    }

    /**
     * Fold or unfold a group of a list.
     * Stored locally immediately, the DB is written in the background. arrayUnion/arrayRemove
     * change only this one entry, so changes from other devices are kept.
     */
    fun setGroupCollapsed(listId: String, groupId: String, collapsed: Boolean) {
        val groupIds = getCachedCollapsedGroups(listId)
        UserSettingsCache.saveCollapsedGroups(listId, if (collapsed) groupIds + groupId else groupIds - groupId)

        val change = if (collapsed) FieldValue.arrayUnion(groupId) else FieldValue.arrayRemove(groupId)
        writeInBackground(mapOf(FIELD_COLLAPSED_GROUPS to mapOf(listId to change)), "folded groups")
    }

    /**
     * Folded groups of a list from the local cache, available without DB access
     */
    fun getCachedCollapsedGroups(listId: String): Set<String> = UserSettingsCache.getCollapsedGroups(listId)

    /**
     * Load the folded groups of a list from the DB and update the local cache (the DB value always wins).
     * Returns the cached value if no user is signed in or the DB cannot be read.
     */
    suspend fun refreshCollapsedGroups(listId: String): Set<String> {
        if (!isSignedIn())
            return getCachedCollapsedGroups(listId)

        return try {
            val groupIds = load().collapsedGroups[listId].orEmpty()
            UserSettingsCache.saveCollapsedGroups(listId, groupIds)
            groupIds
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to load the folded groups, using the cached ones", e)
            getCachedCollapsedGroups(listId)
        }
    }

    /**
     * Load the document of the signed-in user.
     * Returns default values if the document does not exist yet.
     */
    private suspend fun load(): UserDocument {
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

    private fun parse(snapshot: DocumentSnapshot): UserDocument {
        if (!snapshot.exists())
            return UserDocument()

        val settings = snapshot.get(FIELD_SETTINGS) as? Map<*, *>
        return UserDocument(
            settings = UserSettings(
                customTitle = settings?.get(FIELD_CUSTOM_TITLE) as? String,
                syncLastViewedList = settings?.get(FIELD_SYNC_LAST_VIEWED_LIST) as? Boolean ?: false
            ),
            lastViewedListId = snapshot.getString(FIELD_LAST_VIEWED_LIST_ID),
            collapsedGroups = parseCollapsedGroups(snapshot.get(FIELD_COLLAPSED_GROUPS) as? Map<*, *>)
        )
    }

    // { <listId>: [<groupId>, ...] }, entries of an unexpected type are ignored
    private fun parseCollapsedGroups(map: Map<*, *>?): Map<String, Set<String>> {
        return map.orEmpty()
            .mapNotNull { (listId, groupIds) ->
                if (listId !is String || groupIds !is List<*>)
                    null
                else
                    listId to groupIds.filterIsInstance<String>().toSet()
            }
            .toMap()
    }
}
