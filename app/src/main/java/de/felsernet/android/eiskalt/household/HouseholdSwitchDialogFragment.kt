package de.felsernet.android.eiskalt.household

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import de.felsernet.android.eiskalt.R

/**
 * Choose the household to work with on this device: from the menu (only offered with more than
 * one household) or on its own, if none is chosen yet (see HouseholdState.Choose).
 * Goes back to the list overview, so no list of the previous household stays open.
 */
class HouseholdSwitchDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val households = HouseholdRepository.households
        val names = households.map { it.name }.toTypedArray()
        // nothing checked if no household is chosen yet
        val checked = households.indexOfFirst { it.id == HouseholdRepository.activeHousehold?.id }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.household_switch_title)
            .setSingleChoiceItems(names, checked) { dialog, which ->
                val household = households[which]
                if (household.id != HouseholdRepository.activeHousehold?.id) {
                    HouseholdRepository.switchTo(household)
                    requireActivity().findNavController(R.id.nav_host_fragment_content_main)
                        .popBackStack(R.id.AllListsFragment, false)
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    companion object {
        const val TAG = "HouseholdSwitchDialog"

        /**
         * Show the dialog, unless it is shown already (e.g. opened on its own and from the menu)
         */
        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null)
                HouseholdSwitchDialogFragment().show(fragmentManager, TAG)
        }
    }
}
