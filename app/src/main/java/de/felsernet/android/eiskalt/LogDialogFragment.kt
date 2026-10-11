package de.felsernet.android.eiskalt

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import de.felsernet.android.eiskalt.databinding.DialogLogBinding

/**
 * Simple dialog showing the content of AppLog.
 */
class LogDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogLogBinding.inflate(layoutInflater)
        binding.textViewLog.text = AppLog.getText().ifEmpty { getString(R.string.log_empty) }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.log_title)
            .setView(binding.root)
            .setPositiveButton(R.string.log_close, null)
            .setNeutralButton(R.string.log_clear) { _, _ -> AppLog.clear() }
            .create()
    }

    companion object {
        const val TAG = "LogDialog"
    }
}
