package de.felsernet.android.eiskalt.base

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

/**
 * The own changes that the server has not confirmed yet (e.g. made offline), to show them.
 * Only kept in memory: after a restart Firestore still syncs the older changes, but they are not listed.
 */
object PendingWrites {

    enum class Operation { SAVE, UPDATE, DELETE }

    data class Entry(
        val id: Long,
        val operation: Operation,
        val name: String,
        val time: Date,
        val failed: Boolean = false
    )

    private val nextId = AtomicLong()

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    /**
     * List a write until the server has confirmed it; a failed write stays listed (marked as failed)
     */
    fun track(task: Task<Void>, operation: Operation, name: String) {
        val entry = Entry(nextId.incrementAndGet(), operation, name, Date())
        _entries.update { it + entry }
        task.addOnSuccessListener {
            _entries.update { list -> list.filterNot { it.id == entry.id } }
        }
        task.addOnFailureListener {
            _entries.update { list -> list.map { if (it.id == entry.id) it.copy(failed = true) else it } }
        }
    }
}
