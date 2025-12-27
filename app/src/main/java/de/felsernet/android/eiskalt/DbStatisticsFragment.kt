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
                is AuthManager.AuthState.Authenticated -> "Authenticated (${authState.user.email})"
                is AuthManager.AuthState.Unauthenticated -> "Not Authenticated"
            }
            binding.textViewAuthStatus.text = "Authentication Status: $status"
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val repository = InventoryRepository()
                binding.textViewConnectionStatus.text = "Connection Status: Connected"

                val listNames = repository.getAllListNames()
                val totalLists = listNames.size
                binding.textViewTotalLists.text = "Total Lists: $totalLists"

                var totalItems = 0
                for (listName in listNames) {
                    val items = repository.getList(listName)
                    totalItems += items.size
                }
                binding.textViewTotalItems.text = "Total Items: $totalItems"

            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(requireContext(), e, "load statistics")
                binding.textViewConnectionStatus.text = "Connection Status: Error"
            } catch (e: Exception) {
                binding.textViewConnectionStatus.text = "Connection Status: Error"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
