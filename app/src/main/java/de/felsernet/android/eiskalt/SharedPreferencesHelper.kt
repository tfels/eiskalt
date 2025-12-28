package de.felsernet.android.eiskalt

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesHelper {
    private const val PREFS_NAME = "EiskaltPrefs"
    private const val KEY_LAST_VIEWED_LIST = "last_viewed_list"

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLastViewedList(listName: String) {
        sharedPreferences.edit().putString(KEY_LAST_VIEWED_LIST, listName).apply()
    }

    fun getLastViewedList(): String? {
        return sharedPreferences.getString(KEY_LAST_VIEWED_LIST, null)
    }

    fun clearLastViewedList() {
        sharedPreferences.edit().remove(KEY_LAST_VIEWED_LIST).apply()
    }
}
