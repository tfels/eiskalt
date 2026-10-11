package de.felsernet.android.eiskalt

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import de.felsernet.android.eiskalt.databinding.DialogLogBinding
import kotlinx.coroutines.launch

/**
 * Simple dialog showing the content of AppLog.
 * Each entry uses two lines: header (date, severity, tag) and the indented message.
 */
class LogDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogLogBinding.inflate(layoutInflater)

        // update the text whenever entries are added or cleared while the dialog is visible
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppLog.entries.collect { entries -> showEntries(binding, entries) }
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.log_title)
            .setView(binding.root)
            .setPositiveButton(R.string.log_close, null)
            .setNeutralButton(R.string.log_clear, null)
            .create()

        // set the click listener after show(), so "Clear" does not close the dialog
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { AppLog.clear() }
        }
        return dialog
    }

    private fun showEntries(binding: DialogLogBinding, entries: List<AppLog.Entry>) {
        val scrollView = binding.root
        // follow new entries only if the user has not scrolled up
        val wasAtBottom = !scrollView.canScrollVertically(1)

        binding.textViewLog.text = if (entries.isEmpty()) getString(R.string.log_empty) else formatEntries(entries)

        if (wasAtBottom) {
            scrollView.post { scrollView.scrollTo(0, binding.textViewLog.bottom) }
        }
    }

    private fun formatEntries(entries: List<AppLog.Entry>): CharSequence {
        val indent = resources.getDimensionPixelSize(R.dimen.log_message_indent)
        val text = SpannableStringBuilder()

        for ((index, entry) in entries.withIndex()) {
            if (index > 0) text.append("\n")

            // header line in bold, colored by severity
            val headerStart = text.length
            text.append(entry.header).append("\n")
            text.setSpan(StyleSpan(Typeface.BOLD), headerStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(ForegroundColorSpan(levelColor(entry.level)), headerStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // message indented, also on wrapped lines
            val messageStart = text.length
            text.append(entry.message)
            text.setSpan(LeadingMarginSpan.Standard(indent), messageStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return text
    }

    private fun levelColor(level: AppLog.Level): Int {
        val colorRes = when (level) {
            AppLog.Level.INFO -> R.color.log_info
            AppLog.Level.WARN -> R.color.log_warn
            AppLog.Level.ERROR -> R.color.log_error
        }
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    companion object {
        const val TAG = "LogDialog"
    }
}
