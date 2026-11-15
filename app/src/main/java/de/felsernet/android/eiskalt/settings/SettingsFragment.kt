package de.felsernet.android.eiskalt.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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

        // Show the current settings (from the local cache)
        val settings = UserSettingsRepository.getCachedSettings()
        binding.switchSyncLastViewedList.isChecked = settings.syncLastViewedList

        binding.buttonSave.setOnClickListener {
            val userSettings = UserSettingsRepository.UserSettings(
                syncLastViewedList = binding.switchSyncLastViewedList.isChecked
            )
            UserSettingsRepository.saveSettings(userSettings)
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
