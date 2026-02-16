package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var currentObject: T

    // implementations might override ui element variables to prevent auto detection,
    // or modify in setupSpecificGuiElements to prevent initialization
    protected open var buttonSave: Button? = null
    protected open var editTextName: EditText? = null
    protected open var textViewId: TextView? = null
    protected open var recyclerViewIcons: RecyclerView? = null
    protected open var editTextComment: EditText? = null

    protected open val iconFilePrefix: String = ""

    abstract fun getCurrentObject(): T
    abstract fun getSpecificChanges(obj: T)
    abstract fun setupSpecificGuiElements(obj: T)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentObject = getCurrentObject()

        // Set the title
        val title = currentObject.name.ifEmpty { newObjectTitle }
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title

        // Look for common UI elements if not overridden in derived class
        editTextName = editTextName ?: view.findViewById(R.id.editTextName)
        textViewId = textViewId ?: view.findViewById(R.id.textViewId)
        recyclerViewIcons = recyclerViewIcons ?: view.findViewById(R.id.recyclerViewIcons)
        editTextComment = editTextComment ?: view.findViewById(R.id.editTextComment)
        buttonSave = buttonSave ?: view.findViewById(R.id.buttonSave)

        // let the derived class initialize its ui
        setupSpecificGuiElements(currentObject)

        // initialize ui elements if they are valid
        editTextName?.setText(currentObject.name)
        textViewId?.text = currentObject.id.ifEmpty { "New" }
        setupIconSelector()
        editTextComment?.setText(currentObject.comment)
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

    // setup the recyclerView for selecting an item
    protected fun setupIconSelector() {
        if(recyclerViewIcons == null)
            return

        val iconAdapter = IconSelectorAdapter(iconFilePrefix)

        recyclerViewIcons!!.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = iconAdapter
        }

        // Set current selection if an icon is already set
        iconAdapter.setSelectedIcon(currentObject.icon)
    }

    private fun saveChanges() {
        currentObject.name = editTextName?.text.toString().trim()
        currentObject.comment = editTextComment?.text?.toString()?.trim().orEmpty()
        currentObject.icon = (recyclerViewIcons?.adapter as? IconSelectorAdapter)?.getSelectedIcon()

        getSpecificChanges(currentObject)

        // Save via ViewModel (validation handled in ViewModel)
        viewModel.saveObject(currentObject)
    }
}
