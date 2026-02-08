package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

/**
 * Base fragment for details screens (add/edit) handling common patterns:
 * - Save button handling
 * - Back button callback with save
 */
abstract class BaseDetailsFragment<T: BaseDataClass> : Fragment() {
    // Shared ViewModels survives fragment recreation
    protected val sharedMessageViewModel: SharedMessageViewModel by activityViewModels()
    abstract val viewModel: BaseViewModel<T>
    protected abstract val newObjectTitle: String
    protected lateinit var currentObject: T

    // implementations might override ui element variables to prevent auto detection
    protected var buttonSave: Button? = null

    abstract fun setCurrentObject()
    abstract fun saveChanges()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setCurrentObject()

        // Set the title
        val title = currentObject.name.ifEmpty { newObjectTitle }
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title

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

        // Collect all flows in a single repeatOnLifecycle block
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.navigateBack.collect {
                        findNavController().navigateUp()
                    }
                }
                // collect concurrently so both collectors run
                launch {
                    sharedMessageViewModel.errorMessage.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
                launch {
                    sharedMessageViewModel.successMessage.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
