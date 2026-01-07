package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestoreException
import de.felsernet.android.eiskalt.ListFragmentUtils.handleFirestoreException
import de.felsernet.android.eiskalt.databinding.FragmentDbStatisticsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DbStatisticsFragment : Fragment() {

    private var _binding: FragmentDbStatisticsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDbStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadStatistics()

        // Observe auth state
        AuthManager.authState.observe(viewLifecycleOwner) { authState ->
            when (authState) {
                is AuthManager.AuthState.Authenticated -> {
                    val user = authState.user
                    binding.textViewAuthStatus.text = getString(R.string.auth_status) + getString(R.string.auth_authenticated) + " (${user.email})"

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val creationDate = Date(user.metadata?.creationTimestamp ?: 0)
                    val lastSignInDate = Date(user.metadata?.lastSignInTimestamp ?: 0)

                    binding.textViewUserCreation.text = getString(R.string.account_created) + dateFormat.format(creationDate)
                    binding.textViewLastSignIn.text = getString(R.string.last_signin) + dateFormat.format(lastSignInDate)
                    binding.textViewAuthProvider.text = getString(R.string.auth_provider) + (user.providerData.firstOrNull()?.providerId ?: getString(R.string.unknown_value))
                }
                is AuthManager.AuthState.Unauthenticated -> {
                    binding.textViewAuthStatus.text = getString(R.string.auth_status) + getString(R.string.auth_not_authenticated)
                    binding.textViewUserCreation.text = getString(R.string.account_created) + getString(R.string.na_value)
                    binding.textViewLastSignIn.text = getString(R.string.last_signin) + getString(R.string.na_value)
                    binding.textViewAuthProvider.text = getString(R.string.auth_provider) + getString(R.string.na_value)
                }
            }
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val repository = ListRepository()
                binding.textViewConnectionStatus.text = getString(R.string.connection_status) + getString(R.string.connection_connected)

                val listNames = repository.getAllListNames()
                val totalLists = listNames.size
                binding.textViewTotalLists.text = getString(R.string.total_lists) + totalLists

                var totalItems = 0
                for (listName in listNames) {
                    totalItems += repository.getItemCount(listName)
                }
                binding.textViewTotalItems.text = getString(R.string.total_items) + totalItems

                binding.textViewReadOperations.text = getString(R.string.db_read_operations) + ListRepository.readOperations
                binding.textViewWriteOperations.text = getString(R.string.db_write_operations) + ListRepository.writeOperations

            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(requireContext(), e, "load statistics")
                binding.textViewConnectionStatus.text = getString(R.string.connection_status) + getString(R.string.connection_error)
            } catch (e: Exception) {
                binding.textViewConnectionStatus.text = getString(R.string.connection_status) + getString(R.string.connection_error)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
