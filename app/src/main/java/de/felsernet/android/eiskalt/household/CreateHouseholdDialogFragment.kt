package de.felsernet.android.eiskalt.household

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestoreException
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Create a new household (the user becomes its owner and first member) and switch to it.
 * Only registered users may do this (see the rules), i.e. users who joined a household before.
 */
class CreateHouseholdDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val padding = (24 * resources.displayMetrics.density).toInt()

        val editText = TextInputEditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(100))
        }
        val inputLayout = TextInputLayout(activity).apply {
            hint = getString(R.string.household_name_hint)
            setPadding(padding, padding / 2, padding, 0)
            addView(editText)
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.household_create_title)
            .setView(inputLayout)
            // the click listener is set in setOnShowListener, so the dialog stays open for an empty name
            .setPositiveButton(R.string.household_create, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = editText.text.toString().trim()
                if (name.isEmpty()) {
                    inputLayout.error = getString(R.string.household_name_empty)
                } else {
                    create(activity, name)
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    // the dialog is closed already: run in the scope of the activity and report with a Toast
    private fun create(activity: FragmentActivity, name: String) {
        activity.lifecycleScope.launch {
            val message = try {
                HouseholdRepository.createHousehold(name)
                // the lists of the new household
                activity.findNavController(R.id.nav_host_fragment_content_main).popBackStack(R.id.AllListsFragment, false)
                activity.getString(R.string.household_created, name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to create the household", e)
                // the rules only allow registered users to create households
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
                    activity.getString(R.string.household_create_not_registered)
                else
                    activity.getString(R.string.household_create_error, e.message)
            }
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val TAG = "CreateHouseholdDialog"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null)
                CreateHouseholdDialogFragment().show(fragmentManager, TAG)
        }
    }
}
