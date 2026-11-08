package de.felsernet.android.eiskalt.household

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.databinding.FragmentHouseholdBinding
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * The active household: its name (every member may rename it) and its members:
 * every member may leave (except the owner), the owner may remove members and hand over the ownership.
 * Only reachable from the menu while a household is active.
 */
class HouseholdFragment : Fragment() {

    private var _binding: FragmentHouseholdBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHouseholdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // e.g. signed out meanwhile
        val household = HouseholdRepository.activeHousehold
        if (household == null) {
            findNavController().navigateUp()
            return
        }

        binding.editTextName.setText(household.name)
        binding.buttonRename.setOnClickListener { rename() }

        showMembers(household)
    }

    private fun rename() {
        val name = binding.editTextName.text.toString().trim()
        if (name.isEmpty()) {
            binding.inputLayoutName.error = getString(R.string.household_name_empty)
            return
        }
        binding.inputLayoutName.error = null

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                HouseholdRepository.renameActiveHousehold(name)
                Snackbar.make(binding.root, R.string.household_renamed, Snackbar.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to rename the household", e)
                Snackbar.make(binding.root, getString(R.string.household_rename_error, e.message), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * List the members by name, marking the owner and the user itself.
     * A member can be tapped if there is an action for it (see onMemberClicked).
     */
    private fun showMembers(household: Household) {
        binding.textViewMembers.setText(R.string.list_loading)
        binding.textViewMembers.visibility = View.VISIBLE
        binding.layoutMembers.removeAllViews()
        val myUid = FirebaseAuth.getInstance().currentUser?.uid

        viewLifecycleOwner.lifecycleScope.launch {
            val members = try {
                HouseholdRepository.getMembers(household.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to load the members", e)
                binding.textViewMembers.setText(R.string.household_members_error)
                return@launch
            }

            binding.textViewMembers.visibility = View.GONE
            val isOwner = household.ownerId == myUid
            for (member in members) {
                val marks = listOfNotNull(
                    getString(R.string.household_member_owner).takeIf { member.uid == household.ownerId },
                    getString(R.string.household_member_you).takeIf { member.uid == myUid }
                )
                val text = if (marks.isEmpty())
                    getString(R.string.household_member_entry, member.name)
                else
                    getString(R.string.household_member_entry_marked, member.name, marks.joinToString(", "))
                // only the owner has actions for other members
                val onClick = if (member.uid == myUid || isOwner) { { onMemberClicked(household, member, myUid) } } else null
                binding.layoutMembers.addView(createRow(text, onClick))
            }
        }
    }

    /**
     * A row of a list on this screen, tappable if onClick is given
     */
    private fun createRow(text: String, onClick: (() -> Unit)?): TextView {
        val padding = (8 * resources.displayMetrics.density).toInt()
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 16f
            setPadding(0, padding, 0, padding)
            if (onClick != null) {
                // the ripple of the theme as click feedback
                val background = TypedValue()
                requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
                setBackgroundResource(background.resourceId)
                setOnClickListener { onClick() }
            }
        }
    }

    /**
     * Actions for a member: leave the household (the user itself),
     * make owner or remove (other members, only for the owner)
     */
    private fun onMemberClicked(household: Household, member: HouseholdMember, myUid: String?) {
        val dialog = AlertDialog.Builder(requireContext())
        when {
            member.uid == myUid && household.ownerId == myUid -> dialog
                .setMessage(R.string.household_owner_cannot_leave)
                .setPositiveButton(android.R.string.ok, null)
            member.uid == myUid -> dialog
                .setMessage(getString(R.string.household_leave_message, household.name))
                .setPositiveButton(R.string.household_leave) { _, _ -> leave(household) }
                .setNegativeButton(android.R.string.cancel, null)
            else -> dialog
                .setTitle(member.name)
                .setItems(arrayOf(getString(R.string.household_member_make_owner), getString(R.string.household_member_remove))) { _, which ->
                    if (which == 0) confirmHandOver(member) else confirmRemove(member)
                }
                .setNegativeButton(android.R.string.cancel, null)
        }
        dialog.show()
    }

    private fun confirmHandOver(member: HouseholdMember) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.household_make_owner_message, member.name))
            .setPositiveButton(R.string.household_member_make_owner) { _, _ ->
                runMemberAction(getString(R.string.household_made_owner, member.name)) {
                    HouseholdRepository.handOverOwnership(member)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmRemove(member: HouseholdMember) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.household_remove_message, member.name))
            .setPositiveButton(R.string.household_member_remove) { _, _ ->
                runMemberAction(getString(R.string.household_removed, member.name)) {
                    HouseholdRepository.removeMember(member)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * Run an action of the owner, then show the message and the changed members
     */
    private fun runMemberAction(successMessage: String, action: suspend () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                action()
                Snackbar.make(binding.root, successMessage, Snackbar.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to change the members", e)
                Snackbar.make(binding.root, getString(R.string.household_member_action_error, e.message), Snackbar.LENGTH_LONG).show()
            }
            // e.g. with the new owner
            HouseholdRepository.activeHousehold?.let { showMembers(it) }
        }
    }

    /**
     * Leave the household and go back to the list overview
     * (it shows the remaining household, the choice or the message without household)
     */
    private fun leave(household: Household) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                HouseholdRepository.leaveActiveHousehold()
                Snackbar.make(binding.root, getString(R.string.household_left, household.name), Snackbar.LENGTH_LONG).show()
                findNavController().popBackStack(R.id.AllListsFragment, false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to leave the household", e)
                Snackbar.make(binding.root, getString(R.string.household_leave_error, e.message), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
