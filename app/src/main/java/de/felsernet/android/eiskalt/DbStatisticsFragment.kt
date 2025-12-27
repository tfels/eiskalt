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
            val status = when (authState) {
                is AuthManager.AuthState.Authenticated -> getString(R.string.auth_authenticated) + " (${authState.user.email})"
                is AuthManager.AuthState.Unauthenticated -> getString(R.string.auth_not_authenticated)
            }
            binding.textViewAuthStatus.text = getString(R.string.auth_status) + status
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val repository = InventoryRepository()
                binding.textViewConnectionStatus.text = getString(R.string.connection_status) + getString(R.string.connection_connected)

                val listNames = repository.getAllListNames()
                val totalLists = listNames.size
                binding.textViewTotalLists.text = getString(R.string.total_lists) + totalLists

                var totalItems = 0
                for (listName in listNames) {
                    val items = repository.getList(listName)
                    totalItems += items.size
                }
                binding.textViewTotalItems.text = getString(R.string.total_items) + totalItems

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
