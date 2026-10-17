package de.felsernet.android.eiskalt.log

import android.text.format.DateFormat
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory application log, shown in the log dialog.
 * Every entry is also forwarded to logcat.
 * Only the last MAX_ENTRIES entries are kept; nothing is persisted.
 * Entries are exposed as StateFlow, so observers get updates while they are active.
 */
object AppLog {
    private const val MAX_ENTRIES = 200

    enum class Level(val letter: String) { INFO("I"), WARN("W"), ERROR("E") }

    /** header: date, severity and tag; message: the log text */
    data class Entry(val level: Level, val header: String, val message: String)

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    // true if an error was logged that the user has not seen in the log dialog yet
    private val _hasUnseenErrors = MutableStateFlow(false)
    val hasUnseenErrors: StateFlow<Boolean> = _hasUnseenErrors.asStateFlow()

    private const val TIME_FORMAT = "HH:mm:ss"

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        add(Level.INFO, tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        add(Level.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        add(Level.ERROR, tag, message, throwable)
    }

    fun clear() {
        _entries.value = emptyList()
        _hasUnseenErrors.value = false
    }

    fun markSeen() {
        _hasUnseenErrors.value = false
    }

    private fun add(level: Level, tag: String, message: String, throwable: Throwable?) {
        val details = throwable?.let { ": ${it.message ?: it.javaClass.simpleName}" } ?: ""
        val entry = Entry(level, "${DateFormat.format(TIME_FORMAT, System.currentTimeMillis())} ${level.letter}/$tag", "$message$details")
        // set the flag before adding the entry, so an open log dialog can reset it again
        if (level == Level.ERROR) {
            _hasUnseenErrors.value = true
        }
        // update() is atomic, so add() can be called from any thread
        _entries.update { (it + entry).takeLast(MAX_ENTRIES) }
    }
}
