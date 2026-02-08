package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import de.felsernet.android.eiskalt.databinding.FragmentItemDetailsBinding
import kotlinx.coroutines.launch

/**
 * Fragment for editing/creating an item.
 * Uses ViewModel with Flows for state management and data sharing.
 */
class ItemDetailsFragment : BaseDetailsFragment<Item>() {
    private var _binding: FragmentItemDetailsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Shared ViewModel survives fragment recreation
    override val viewModel: ItemViewModel by activityViewModels()
    private lateinit var currentItem: Item
    private var groupAdapter: ArrayAdapter<String>? = null
    private lateinit var groups: List<Group>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use SafeArgs to get arguments
        val args = ItemDetailsFragmentArgs.fromBundle(requireArguments())
        currentItem = args.item ?: Item("")

        // Set the title
        val title = if (currentItem.name.isNotEmpty()) currentItem.name else "New Item"
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title

        binding.editTextName.setText(currentItem.name)
        binding.editTextQuantity.setText(currentItem.quantity.toString())

        // Load groups and set up spinner
        setupGroupSpinner()
    }

    private fun setupGroupSpinner() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val groupRepository = GroupRepository.getInstance()
                groups = groupRepository.getAll()

                // Create adapter with group names
                val groupNames = mutableListOf(getString(R.string.no_group))
                groupNames.addAll(groups.map { it.name })

                groupAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, groupNames)
                groupAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerGroup.adapter = groupAdapter

                // Set the current group selection
                if (currentItem.groupId != null) {
                    val selectedIndex = groups.indexOfFirst { it.id == currentItem.groupId }
                    if (selectedIndex >= 0) {
                        binding.spinnerGroup.setSelection(selectedIndex + 1) // +1 for "No Group" option
                    }
                } else {
                    binding.spinnerGroup.setSelection(0) // "No Group"
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading groups: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun saveChanges() {
        val updatedName = binding.editTextName.text.toString().trim()
        val updatedQuantityText = binding.editTextQuantity.text.toString().trim()
        val updatedQuantity = updatedQuantityText.toIntOrNull() ?: 0

        currentItem.name = updatedName
        currentItem.quantity = updatedQuantity

        // Update the group based on spinner selection
        val selectedPosition = binding.spinnerGroup.selectedItemPosition
        currentItem.groupId = if (selectedPosition > 0 && selectedPosition - 1 < groups.size) {
            groups[selectedPosition - 1].id // -1 to account for "No Group" option
        } else {
            null // No group selected
        }

        // Validate and save via ViewModel
        viewModel.saveObject(currentItem)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
