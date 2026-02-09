package de.felsernet.android.eiskalt

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesHelper {
    private const val PREFS_NAME = "EiskaltPrefs"
    private const val KEY_LAST_VIEWED_LIST = "last_viewed_list"
    private const val KEY_CUSTOM_TITLE = "custom_title"

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
}
