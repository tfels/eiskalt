package de.felsernet.android.eiskalt

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import de.felsernet.android.eiskalt.databinding.DialogLogBinding

/**
 * Simple dialog showing the content of AppLog.
 * Each entry uses two lines: header (date, severity, tag) and the indented message.
 */
class LogDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogLogBinding.inflate(layoutInflater)
        val entries = AppLog.getEntries()
        binding.textViewLog.text = if (entries.isEmpty()) getString(R.string.log_empty) else formatEntries(entries)

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.log_title)
            .setView(binding.root)
            .setPositiveButton(R.string.log_close, null)
            .setNeutralButton(R.string.log_clear) { _, _ -> AppLog.clear() }
            .create()
    }

    private fun formatEntries(entries: List<AppLog.Entry>): CharSequence {
        val indent = resources.getDimensionPixelSize(R.dimen.log_message_indent)
        val text = SpannableStringBuilder()

        for ((index, entry) in entries.withIndex()) {
            if (index > 0) text.append("\n")

            // header line in bold
            val headerStart = text.length
            text.append(entry.header).append("\n")
            text.setSpan(StyleSpan(Typeface.BOLD), headerStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // message indented, also on wrapped lines
            val messageStart = text.length
            text.append(entry.message)
            text.setSpan(LeadingMarginSpan.Standard(indent), messageStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return text
    }

    companion object {
        const val TAG = "LogDialog"
    }
}
