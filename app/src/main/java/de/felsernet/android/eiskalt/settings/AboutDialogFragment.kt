package de.felsernet.android.eiskalt.settings

import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import de.felsernet.android.eiskalt.R

/**
 * Simple "About" dialog: app name, version, short description, beta warning and author.
 */
class AboutDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message =
            getString(R.string.about_version, getVersionName()) +
            "\n" + getString(R.string.about_author) +
            "\n\n" + getString(R.string.about_description) +
            "\n\n" + getString(R.string.about_beta_warning)

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.about_close, null)
            .create()
    }

    private fun getVersionName(): String {
        val context = requireContext()
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return packageInfo.versionName ?: "?"
    }

    companion object {
        const val TAG = "AboutDialog"
    }
}
