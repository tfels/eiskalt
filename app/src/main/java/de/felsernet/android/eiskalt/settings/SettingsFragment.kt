package de.felsernet.android.eiskalt.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.databinding.FragmentSettingsBinding
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the settings destination in the navigation.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load current title
        val currentTitle = UserSettingsRepository.getCachedCustomTitle() ?: getString(R.string.all_lists_fragment_default_label)
        binding.editTextTitle.setText(currentTitle)

        binding.buttonSave.setOnClickListener {
            val newTitle = binding.editTextTitle.text.toString().trim().ifEmpty { null }
            saveTitle(newTitle)
            val message = if (newTitle != null) R.string.settings_title_updated else R.string.settings_default_title_used
            Snackbar.make(binding.root, getString(message), Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * Save the title in the DB (and the local cache).
     * Uses the activity scope, so the write is not canceled when this fragment is closed.
     * Firestore completes the write only when the server confirms it, which can take long when offline.
     */
    private fun saveTitle(title: String?) {
        requireActivity().lifecycleScope.launch {
            try {
                UserSettingsRepository.saveCustomTitle(title)
            } catch (e: Exception) {
                AppLog.e("settings", "Failed to save the custom title", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
