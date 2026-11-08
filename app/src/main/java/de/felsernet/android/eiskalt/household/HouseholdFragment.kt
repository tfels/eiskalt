package de.felsernet.android.eiskalt.household

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 * The active household: its name (every member may rename it) and its members.
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
     * List the members by name, marking the owner and the user itself
     */
    private fun showMembers(household: Household) {
        binding.textViewMembers.setText(R.string.list_loading)
        val myUid = FirebaseAuth.getInstance().currentUser?.uid

        viewLifecycleOwner.lifecycleScope.launch {
            binding.textViewMembers.text = try {
                HouseholdRepository.getMembers(household.id).joinToString("\n") { member ->
                    val marks = listOfNotNull(
                        getString(R.string.household_member_owner).takeIf { member.uid == household.ownerId },
                        getString(R.string.household_member_you).takeIf { member.uid == myUid }
                    )
                    if (marks.isEmpty())
                        getString(R.string.household_member_entry, member.name)
                    else
                        getString(R.string.household_member_entry_marked, member.name, marks.joinToString(", "))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to load the members", e)
                getString(R.string.household_members_error)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
