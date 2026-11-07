package de.felsernet.android.eiskalt.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseRepository
import de.felsernet.android.eiskalt.common.AuthManager
import de.felsernet.android.eiskalt.common.SharedMessageViewModel
import de.felsernet.android.eiskalt.databinding.FragmentDbStatisticsBinding
import de.felsernet.android.eiskalt.household.HouseholdRepository
import de.felsernet.android.eiskalt.inventory.ListRepository
import de.felsernet.android.eiskalt.item.ItemRepository
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.CancellationException
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
        binding.textViewTotalLists.text = loadingNumber
        binding.textViewTotalItems.text = loadingNumber
        binding.textViewReadOperations.text = loadingNumber
        binding.textViewWriteOperations.text = loadingNumber
        binding.textViewAuthStatus.text = getString(R.string.auth_status, loading)
        binding.textViewHousehold.text = getString(R.string.household_active, loading)
        binding.textViewUserCreation.text = getString(R.string.account_created, loading)
        binding.textViewLastSignIn.text = getString(R.string.last_signin, loading)
        binding.textViewAuthProvider.text = getString(R.string.auth_provider, loading)

        loadStatistics()

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
                    showHousehold()
                }
                is AuthManager.AuthState.Unauthenticated -> {
                    val na = getString(R.string.na_value)
                    binding.textViewAuthStatus.text = getString(R.string.auth_status, getString(R.string.auth_not_authenticated))
                    binding.textViewHousehold.text = getString(R.string.household_active, na)
                    binding.textViewUserCreation.text = getString(R.string.account_created, na)
                    binding.textViewLastSignIn.text = getString(R.string.last_signin, na)
                    binding.textViewAuthProvider.text = getString(R.string.auth_provider, na)
                }
            }
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val listRepository = ListRepository()
                binding.textViewConnectionStatus.text = getString(R.string.connection_status, getString(R.string.connection_connected))

                val listInfos = listRepository.getAll()
                val totalLists = listInfos.size
                binding.textViewTotalLists.text = getString(R.string.total_lists, totalLists)

                var totalItems = 0
                for (listInfo in listInfos) {
                    totalItems += ItemRepository(listInfo).count()
                }
                binding.textViewTotalItems.text = getString(R.string.total_items, totalItems)

                binding.textViewReadOperations.text = getString(R.string.db_read_operations, BaseRepository.getTotalReadOperations())
                binding.textViewWriteOperations.text = getString(R.string.db_write_operations, BaseRepository.getTotalWriteOperations())

            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("statistics", getString(R.string.statistics_load_error, e.message), e)
                binding.textViewConnectionStatus.text = getString(R.string.connection_status, getString(R.string.connection_error))
            }
        }
    }

    /**
     * Show the active household; loaded here if MainActivity has not loaded it yet
     */
    private fun showHousehold() {
        viewLifecycleOwner.lifecycleScope.launch {
            val text = try {
                val household = HouseholdRepository.activeHousehold ?: HouseholdRepository.loadActiveHousehold()
                household?.name ?: getString(R.string.household_none)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("household", "Failed to load the households", e)
                getString(R.string.connection_error)
            }
            binding.textViewHousehold.text = getString(R.string.household_active, text)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
