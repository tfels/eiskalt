package de.felsernet.android.eiskalt.household

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.keepAboveKeyboard
import de.felsernet.android.eiskalt.databinding.FragmentHouseholdBinding
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat

/**
 * The active household: its name (every member may rename it), its members
 * (every member may leave, except the owner; the owner may remove members and hand over the ownership)
 * and the invites (every member may invite others and revoke invites).
 * Only reachable from the menu while a household is active.
 */
class HouseholdFragment : Fragment() {

    private var _binding: FragmentHouseholdBinding? = null
    private val binding get() = _binding!!

    // the invites shown, to avoid inviting the same address twice
    private var invites: List<Invite> = emptyList()

    // display time of the hint after inviting someone (ms), LENGTH_LONG is too short to read it
    private val invitedHintDuration = 8000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHouseholdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.keepAboveKeyboard()

        // e.g. signed out meanwhile
        val household = HouseholdRepository.activeHousehold
        if (household == null) {
            findNavController().navigateUp()
            return
        }

        binding.editTextName.setText(household.name)
        binding.buttonRename.setOnClickListener { rename() }
        binding.buttonInvite.setOnClickListener { invite(household) }

        showMembers(household)
        showInvites(household)
    }

    private fun invite(household: Household) {
        val email = InviteEmail.normalize(binding.editTextInviteEmail.text.toString())
        if (email == null) {
            binding.inputLayoutInviteEmail.error = getString(R.string.household_invite_email_invalid)
            return
        }
        if (invites.any { it.email == email && !it.isExpired() }) {
            binding.inputLayoutInviteEmail.error = getString(R.string.household_invite_exists, email)
            return
        }
        binding.inputLayoutInviteEmail.error = null

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val invite = HouseholdRepository.createInvite(email)
                binding.editTextInviteEmail.text = null
                // a hint the user should read: shown longer and not cut off
                Snackbar.make(binding.root, getString(R.string.household_invited, email), invitedHintDuration)
                    .setTextMaxLines(5)
                    .show()
                showInvites(household)
                // the app can't send it itself: let the user send it (e.g. by e-mail)
                share(household, invite)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to create the invite", e)
                Snackbar.make(binding.root, getString(R.string.household_invite_error, e.message), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * List the invites (with expiry date), tap one to revoke it,
     * the open ones have a button to share them (e.g. by e-mail)
     */
    private fun showInvites(household: Household) {
        binding.textViewInvitesStatus.setText(R.string.list_loading)
        binding.textViewInvitesStatus.visibility = View.VISIBLE
        binding.layoutInvites.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            invites = try {
                HouseholdRepository.getInvites(household.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to load the invites", e)
                binding.textViewInvitesStatus.setText(R.string.household_invites_error)
                return@launch
            }

            if (invites.isEmpty()) {
                binding.textViewInvitesStatus.setText(R.string.household_invites_none)
                return@launch
            }
            binding.textViewInvitesStatus.visibility = View.GONE
            val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
            for (invite in invites) {
                val row = if (invite.isExpired())
                    createInviteRow(getString(R.string.household_invite_entry_expired, invite.email),
                        onClick = { confirmRevoke(household, invite) }, onShare = null)
                else
                    createInviteRow(getString(R.string.household_invite_entry, invite.email, dateFormat.format(invite.expiresAt)),
                        onClick = { confirmRevoke(household, invite) }, onShare = { share(household, invite) })
                binding.layoutInvites.addView(row)
            }
        }
    }

    /**
     * A row of the invite list: the text (click: onClick) and, if onShare is given, a share button
     */
    private fun createInviteRow(text: String, onClick: () -> Unit, onShare: (() -> Unit)?): View {
        val context = requireContext()
        val padding = (8 * resources.displayMetrics.density).toInt()

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(createRow(text, onClick), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            if (onShare != null) {
                addView(ImageButton(context).apply {
                    setImageResource(R.drawable.ic_share)
                    contentDescription = getString(R.string.household_invite_share)
                    setBackgroundResource(themeAttributeResource(android.R.attr.selectableItemBackgroundBorderless))
                    setPadding(padding, padding, padding, padding)
                    setOnClickListener { onShare() }
                })
            }
        }
    }

    // the page of this app in the Play Store (built from the package name, so it is always right)
    private fun playStoreUrl() = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"

    // e.g. the ripple of the theme as click feedback
    private fun themeAttributeResource(attribute: Int): Int {
        val value = TypedValue()
        requireContext().theme.resolveAttribute(attribute, value, true)
        return value.resourceId
    }

    /**
     * Share the invite (e.g. by e-mail or messenger), the app itself can't send e-mails.
     * E-mail apps take the address of the invite as recipient.
     */
    private fun share(household: Household, invite: Invite) {
        val validUntil = DateFormat.getDateInstance(DateFormat.MEDIUM).format(invite.expiresAt)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(invite.email))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.household_invite_share_subject, household.name))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.household_invite_share_text,
                household.name, invite.email, validUntil, playStoreUrl()))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.household_invite_share)))
    }

    private fun confirmRevoke(household: Household, invite: Invite) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.household_invite_revoke_message, invite.email))
            .setPositiveButton(R.string.household_invite_revoke) { _, _ -> revoke(household, invite) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun revoke(household: Household, invite: Invite) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                HouseholdRepository.revokeInvite(invite)
                Snackbar.make(binding.root, R.string.household_invite_revoked, Snackbar.LENGTH_SHORT).show()
                showInvites(household)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to revoke the invite", e)
                Snackbar.make(binding.root, getString(R.string.household_invite_revoke_error, e.message), Snackbar.LENGTH_LONG).show()
            }
        }
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
                setBackgroundResource(themeAttributeResource(android.R.attr.selectableItemBackground))
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
