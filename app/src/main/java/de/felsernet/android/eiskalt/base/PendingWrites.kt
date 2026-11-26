package de.felsernet.android.eiskalt.base

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import de.felsernet.android.eiskalt.common.NetworkMonitor
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

/**
 * The own changes that the server has not confirmed yet (e.g. made offline), to show them.
 * Stored on the device, because Firestore keeps unsynced changes across restarts as well.
 * After a restart the entries of the last run are removed together once Firestore has synced
 * all older changes (there is no way to follow them one by one anymore).
 */
object PendingWrites {
    private const val PREFS_NAME = "PendingWrites"
    private const val KEY_ENTRIES = "entries"

    enum class Operation { SAVE, UPDATE, DELETE }

    data class Entry(
        val id: Long,
        val operation: Operation,
        val name: String,
        val time: Date,
        val failed: Boolean = false
    )

    private var sharedPreferences: SharedPreferences? = null
    private var nextId = 0L

    // changes made offline (or in the last run): logged once they are all synced,
    // not those made online (the server confirms them at once)
    private var offlineChanges = 0

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    /**
     * Load the entries of the last run, call once at app start
     */
    @Synchronized
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs
        val restored = prefs.getStringSet(KEY_ENTRIES, null).orEmpty()
            .mapNotNull { decode(it) }
            .sortedBy { it.id }
        _entries.value = restored
        nextId = restored.maxOfOrNull { it.id } ?: 0L
        offlineChanges = restored.size
        if (restored.isEmpty())
            return

        // the writes of the last run can't be followed anymore: wait until Firestore has synced
        // all of them (also on a failure: nothing to wait for anymore)
        val lastRestoredId = nextId
        FirebaseFirestore.getInstance().waitForPendingWrites().addOnCompleteListener {
            setEntries(_entries.value.filterNot { it.id <= lastRestoredId })
        }
    }

    /**
     * List a write until the server has confirmed it; a failed write stays listed (marked as failed)
     */
    @Synchronized
    fun track(task: Task<Void>, operation: Operation, name: String) {
        val entry = Entry(++nextId, operation, name, Date())
        if (!NetworkMonitor.isOnline.value)
            offlineChanges++
        setEntries(_entries.value + entry)
        task.addOnSuccessListener {
            setEntries(_entries.value.filterNot { it.id == entry.id })
        }
        task.addOnFailureListener {
            setEntries(_entries.value.map { if (it.id == entry.id) it.copy(failed = true) else it })
        }
    }

    /**
     * Forget all entries, e.g. after signing out (they belong to the user)
     */
    @Synchronized
    fun clear() {
        offlineChanges = 0
        setEntries(emptyList())
    }

    @Synchronized
    private fun setEntries(entries: List<Entry>) {
        _entries.value = entries
        sharedPreferences?.edit { putStringSet(KEY_ENTRIES, entries.map { encode(it) }.toSet()) }

        // a failed change stays listed (and is logged as an error), so this is only logged without one
        if (entries.isEmpty() && offlineChanges > 0) {
            AppLog.i("db", "$offlineChanges offline changes synced")
            offlineChanges = 0
        }
    }

    // "id|operation|time|failed|name", the name last, so it may contain any character
    internal fun encode(entry: Entry) =
        "${entry.id}|${entry.operation}|${entry.time.time}|${entry.failed}|${entry.name}"

    // null for an entry that can't be read (e.g. stored by another app version)
    internal fun decode(text: String): Entry? {
        val parts = text.split("|", limit = 5)
        if (parts.size != 5)
            return null
        return Entry(
            id = parts[0].toLongOrNull() ?: return null,
            operation = Operation.entries.find { it.name == parts[1] } ?: return null,
            time = Date(parts[2].toLongOrNull() ?: return null),
            failed = parts[3].toBooleanStrictOrNull() ?: return null,
            name = parts[4]
        )
    }
}
