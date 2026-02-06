package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Base fragment for details screens (add/edit) handling common patterns:
 * - Save button handling
 * - Back button callback with save
 */
abstract class BaseDetailsFragment : Fragment() {

    // implementations might override ui element variables to prevent auto detection
    protected var buttonSave: Button? = null

    abstract fun saveChanges()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up save button
        if(buttonSave == null)
            buttonSave = view.findViewById(R.id.buttonSave)
        buttonSave?.setOnClickListener {
            saveChanges()
            // Navigation is handled after successful save
        }

        // Handle back button press to save changes
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            saveChanges()
            findNavController().navigateUp()
        }
    }
}
