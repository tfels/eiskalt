package de.felsernet.android.eiskalt.household

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat

/**
 * An open invite for the user (the first one of HouseholdRepository.myInvites):
 * accept it (join the household and switch to it), decline it, or decide later.
 * Shown on its own after signing in (see MainActivity), one invite after the other.
 */
class InviteDialogFragment : DialogFragment() {

    private var invite: Invite? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        // null e.g. if accepted on another device meanwhile, closed in onStart
        val invite = HouseholdRepository.myInvites.value.firstOrNull()
            ?: return AlertDialog.Builder(activity).create()
        this.invite = invite

        val validUntil = DateFormat.getDateInstance(DateFormat.MEDIUM).format(invite.expiresAt)
        // invites created before the names were added don't have them
        val message = if (invite.householdName.isNotEmpty() && invite.invitedByName.isNotEmpty())
            getString(R.string.invite_received_message, invite.invitedByName, invite.householdName, validUntil)
        else
            getString(R.string.invite_received_message_without_names, validUntil)

        return AlertDialog.Builder(activity)
            .setTitle(R.string.invite_received_title)
            .setMessage(message)
            .setPositiveButton(R.string.invite_accept) { _, _ -> accept(activity, invite) }
            .setNegativeButton(R.string.invite_decline) { _, _ -> decline(activity, invite) }
            .setNeutralButton(R.string.invite_later) { _, _ -> postpone(activity) }
            .create()
    }

    override fun onStart() {
        super.onStart()
        if (invite == null)
            dismissAllowingStateLoss()
    }

    // "Later": don't show the invites on their own until the next start,
    // but let the user choose a household now if needed (it waited for the invites, see MainActivity)
    private fun postpone(activity: FragmentActivity) {
        HouseholdRepository.invitesPostponed = true
        if (HouseholdRepository.state.value is HouseholdState.Choose)
            HouseholdSwitchDialogFragment.show(activity.supportFragmentManager)
    }

    // the dialog is closed already: run in the scope of the activity and report with a Toast
    private fun accept(activity: FragmentActivity, invite: Invite) {
        activity.lifecycleScope.launch {
            try {
                HouseholdRepository.acceptInvite(invite)
                showToast(activity, if (invite.householdName.isNotEmpty())
                    activity.getString(R.string.invite_accepted, invite.householdName)
                else
                    activity.getString(R.string.invite_accepted_without_name))
                // the lists of the new household
                activity.findNavController(R.id.nav_host_fragment_content_main).popBackStack(R.id.AllListsFragment, false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to accept the invite", e)
                showToast(activity, activity.getString(R.string.invite_accept_error, e.message))
            }
        }
    }

    private fun decline(activity: FragmentActivity, invite: Invite) {
        activity.lifecycleScope.launch {
            try {
                HouseholdRepository.declineInvite(invite)
                showToast(activity, activity.getString(R.string.invite_declined))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to decline the invite", e)
                showToast(activity, activity.getString(R.string.invite_decline_error, e.message))
            }
        }
    }

    private fun showToast(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val TAG = "InviteDialog"

        /**
         * Show the dialog, unless it is shown already
         */
        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null)
                InviteDialogFragment().show(fragmentManager, TAG)
        }
    }
}
