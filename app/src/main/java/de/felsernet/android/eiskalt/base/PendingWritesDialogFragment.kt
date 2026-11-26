package de.felsernet.android.eiskalt.base

import android.app.Dialog
import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.felsernet.android.eiskalt.R

/**
 * Lists the own changes the server has not confirmed yet (see PendingWrites), newest first
 */
class PendingWritesDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val timeFormat = DateFormat.getTimeFormat(requireContext())
        val lines = PendingWrites.entries.value.reversed().map { entry ->
            val text = getString(when (entry.operation) {
                PendingWrites.Operation.SAVE -> R.string.pending_write_save
                PendingWrites.Operation.UPDATE -> R.string.pending_write_update
                PendingWrites.Operation.DELETE -> R.string.pending_write_delete
            }, entry.name)
            val line = "${timeFormat.format(entry.time)}  $text"
            if (entry.failed) getString(R.string.pending_write_failed, line) else line
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pending_writes_title)
            .setMessage(lines.joinToString("\n").ifEmpty { getString(R.string.pending_writes_none) })
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    companion object {
        const val TAG = "PendingWritesDialog"
    }
}
