package de.felsernet.android.eiskalt

import android.text.format.DateFormat
import android.util.Log

/**
 * In-memory application log, shown in the log dialog.
 * Every entry is also forwarded to logcat.
 * Only the last MAX_ENTRIES entries are kept; nothing is persisted.
 */
object AppLog {
    private const val MAX_ENTRIES = 200

    private val entries = ArrayDeque<String>()
    private const val TIME_FORMAT = "HH:mm:ss"

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        add("I", tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        add("W", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        add("E", tag, message, throwable)
    }

    @Synchronized
    fun getText(): String = entries.joinToString("\n")

    @Synchronized
    fun clear() = entries.clear()

    @Synchronized
    private fun add(level: String, tag: String, message: String, throwable: Throwable?) {
        val details = throwable?.let { ": ${it.message ?: it.javaClass.simpleName}" } ?: ""
        entries.addLast("${DateFormat.format(TIME_FORMAT, System.currentTimeMillis())} $level/$tag: $message$details")
        while (entries.size > MAX_ENTRIES) {
            entries.removeFirst()
        }
    }
}
