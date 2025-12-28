package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import de.felsernet.android.eiskalt.databinding.FragmentSettingsBinding

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
        val currentTitle = SharedPreferencesHelper.getCustomTitle() ?: getString(R.string.inventory_lists_fragment_default_label)
        binding.editTextTitle.setText(currentTitle)

        binding.buttonSave.setOnClickListener {
            val newTitle = binding.editTextTitle.text.toString().trim()
            if (newTitle.isNotEmpty()) {
                SharedPreferencesHelper.saveCustomTitle(newTitle)
                Snackbar.make(binding.root, getString(R.string.settings_title_updated), Snackbar.LENGTH_SHORT).show()
            } else {
                SharedPreferencesHelper.clearCustomTitle()
                Snackbar.make(binding.root, getString(R.string.settings_default_title_used), Snackbar.LENGTH_SHORT).show()
            }
            findNavController().navigateUp()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
