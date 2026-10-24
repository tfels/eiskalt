package de.felsernet.android.eiskalt.settings

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesHelper {
    private const val PREFS_NAME = "EiskaltPrefs"
    private const val KEY_LAST_VIEWED_LIST = "last_viewed_list"
    private const val KEY_CUSTOM_TITLE = "custom_title"
    private const val KEY_SYNC_LAST_VIEWED_LIST = "sync_last_viewed_list"
    private const val KEY_COLLAPSED_GROUPS_PREFIX = "collapsed_groups_"   // + list id

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLastViewedList(listId: String) {
        sharedPreferences.edit().putString(KEY_LAST_VIEWED_LIST, listId).apply()
    }

    fun getLastViewedList(): String? {
        return sharedPreferences.getString(KEY_LAST_VIEWED_LIST, null)
    }

    fun clearLastViewedList() {
        sharedPreferences.edit().remove(KEY_LAST_VIEWED_LIST).apply()
    }

    fun saveCustomTitle(title: String) {
        sharedPreferences.edit().putString(KEY_CUSTOM_TITLE, title).apply()
    }

    fun getCustomTitle(): String? {
        return sharedPreferences.getString(KEY_CUSTOM_TITLE, null)
    }

    fun clearCustomTitle() {
        sharedPreferences.edit().remove(KEY_CUSTOM_TITLE).apply()
    }

    /** Local cache of the user setting (stored in the DB): store the last viewed list in the DB */
    fun setSyncLastViewedList(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SYNC_LAST_VIEWED_LIST, enabled).apply()
    }

    fun isSyncLastViewedList(): Boolean {
        return sharedPreferences.getBoolean(KEY_SYNC_LAST_VIEWED_LIST, false)
    }

    /** Local cache of the folded groups of a list (stored in the DB) */
    fun saveCollapsedGroups(listId: String, groupIds: Set<String>) {
        sharedPreferences.edit().putStringSet(KEY_COLLAPSED_GROUPS_PREFIX + listId, groupIds).apply()
    }

    fun getCollapsedGroups(listId: String): Set<String> {
        // copy, the returned set must not be modified
        return sharedPreferences.getStringSet(KEY_COLLAPSED_GROUPS_PREFIX + listId, null)?.toSet() ?: emptySet()
    }
}
