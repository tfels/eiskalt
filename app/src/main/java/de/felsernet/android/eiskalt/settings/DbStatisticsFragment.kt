package de.felsernet.android.eiskalt.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseRepository
import de.felsernet.android.eiskalt.common.AuthManager
import de.felsernet.android.eiskalt.common.SharedMessageViewModel
import de.felsernet.android.eiskalt.databinding.FragmentDbStatisticsBinding
import de.felsernet.android.eiskalt.group.GroupRepository
import de.felsernet.android.eiskalt.household.HouseholdRepository
import de.felsernet.android.eiskalt.household.HouseholdState
import de.felsernet.android.eiskalt.inventory.ListRepository
import de.felsernet.android.eiskalt.item.ItemRepository
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DbStatisticsFragment : Fragment() {

    private var _binding: FragmentDbStatisticsBinding? = null
    private val binding get() = _binding!!
    protected val sharedMessageViewModel: SharedMessageViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDbStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // show the labels with a placeholder until the values are known,
        // the number lines (%d) can't take the placeholder, so show a loading text there
        val loading = getString(R.string.loading_value)
        val loadingNumber = getString(R.string.list_loading)
        binding.textViewConnectionStatus.text = getString(R.string.connection_status, loading)
        binding.textViewHouseholdRole.text = getString(R.string.household_role, loading)
        binding.textViewHouseholdMembers.text = loadingNumber
        binding.textViewHouseholdInvites.text = loadingNumber
        binding.textViewTotalLists.text = loadingNumber
        binding.textViewTotalItems.text = loadingNumber
        binding.textViewTotalGroups.text = loadingNumber
        binding.textViewReadOperations.text = loadingNumber
        binding.textViewWriteOperations.text = loadingNumber
        binding.textViewAuthStatus.text = getString(R.string.auth_status, loading)
        binding.textViewUserCreation.text = getString(R.string.account_created, loading)
        binding.textViewLastSignIn.text = getString(R.string.last_signin, loading)
        binding.textViewAuthProvider.text = getString(R.string.auth_provider, loading)

        loadStatistics()

        // The active household is loaded by MainActivity after signing in
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                HouseholdRepository.state.collect { state ->
                    val text = when (state) {
                        is HouseholdState.Loading -> loading
                        is HouseholdState.None -> getString(R.string.household_none)
                        is HouseholdState.Choose -> getString(R.string.household_not_chosen)
                        is HouseholdState.Active -> state.household.name
                        is HouseholdState.Error -> getString(R.string.connection_error)
                    }
                    binding.textViewHouseholdTitle.text = getString(R.string.section_household, text)

                    val role = when {
                        state !is HouseholdState.Active -> getString(R.string.na_value)
                        state.household.ownerId == FirebaseAuth.getInstance().currentUser?.uid -> getString(R.string.household_role_owner)
                        else -> getString(R.string.household_role_member)
                    }
                    binding.textViewHouseholdRole.text = getString(R.string.household_role, role)
                    binding.textViewMyHouseholds.text = getString(R.string.household_count, HouseholdRepository.households.size)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                HouseholdRepository.myInvites.collect { invites ->
                    binding.textViewMyInvites.text = getString(R.string.household_my_invites, invites.size)
                }
            }
        }

        // Observe auth state
        AuthManager.authState.observe(viewLifecycleOwner) { authState ->
            when (authState) {
                is AuthManager.AuthState.Authenticated -> {
                    val user = authState.user
                    binding.textViewAuthStatus.text = getString(R.string.auth_status, getString(R.string.auth_authenticated, user.email))

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val creationDate = Date(user.metadata?.creationTimestamp ?: 0)
                    val lastSignInDate = Date(user.metadata?.lastSignInTimestamp ?: 0)

                    binding.textViewUserCreation.text = getString(R.string.account_created, dateFormat.format(creationDate))
                    binding.textViewLastSignIn.text = getString(R.string.last_signin, dateFormat.format(lastSignInDate))
                    binding.textViewAuthProvider.text = getString(R.string.auth_provider, user.providerData.firstOrNull()?.providerId ?: getString(R.string.unknown_value))
                }
                is AuthManager.AuthState.Unauthenticated -> {
                    val na = getString(R.string.na_value)
                    binding.textViewAuthStatus.text = getString(R.string.auth_status, getString(R.string.auth_not_authenticated))
                    binding.textViewUserCreation.text = getString(R.string.account_created, na)
                    binding.textViewLastSignIn.text = getString(R.string.last_signin, na)
                    binding.textViewAuthProvider.text = getString(R.string.auth_provider, na)
                }
            }
        }
    }

    private fun loadStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            // the lists are read from the active household, wait until it is known
            val household = HouseholdRepository.state.filterIsInstance<HouseholdState.Active>().first().household
            try {
                // counted by the server, so it fails offline (the lists may come from the local cache)
                binding.textViewHouseholdMembers.text = getString(R.string.household_member_count,
                    HouseholdRepository.countMembers(household.id))
                binding.textViewHouseholdInvites.text = getString(R.string.household_open_invites,
                    HouseholdRepository.getInvites(household.id).count { !it.isExpired() })

                val listRepository = ListRepository()
                val listInfos = listRepository.getAll()
                val totalLists = listInfos.size
                binding.textViewTotalLists.text = getString(R.string.total_lists, totalLists)

                var totalItems = 0
                for (listInfo in listInfos) {
                    totalItems += ItemRepository(listInfo).count()
                }
                binding.textViewTotalItems.text = getString(R.string.total_items, totalItems)

                binding.textViewTotalGroups.text = getString(R.string.total_groups, GroupRepository.getInstance().count())

                binding.textViewReadOperations.text = getString(R.string.db_read_operations, BaseRepository.getTotalReadOperations())
                binding.textViewWriteOperations.text = getString(R.string.db_write_operations, BaseRepository.getTotalWriteOperations())

                // only now: at least the members were counted by the server
                binding.textViewConnectionStatus.text = getString(R.string.connection_status, getString(R.string.connection_connected))
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("statistics", getString(R.string.statistics_load_error, e.message), e)
                binding.textViewConnectionStatus.text = getString(R.string.connection_status, getString(R.string.connection_error))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
