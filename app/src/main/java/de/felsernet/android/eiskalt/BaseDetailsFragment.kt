package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Base fragment for details screens (add/edit) handling common patterns:
 * - Back button callback with save
 */
abstract class BaseDetailsFragment : Fragment() {

    abstract fun saveChanges()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle back button press to save changes
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            saveChanges()
            findNavController().navigateUp()
        }
    }
}
