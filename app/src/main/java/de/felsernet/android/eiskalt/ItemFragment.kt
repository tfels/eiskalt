package de.felsernet.android.eiskalt

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.felsernet.android.eiskalt.databinding.FragmentItemBinding
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the item destination in the navigation.
 */
class ItemFragment : Fragment() {

    private var _binding: FragmentItemBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var currentItem: Item
    private val groupRepository = GroupRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentItemBinding.inflate(inflater, container, false)
        return binding.root

    }

    private lateinit var groups: List<Group>
    private var groupAdapter: ArrayAdapter<String>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val item = arguments?.getSerializable("item") as? Item
        currentItem = item ?: Item("", quantity = 0)

        // Set the title
        val title = if (currentItem.name.isNotEmpty()) currentItem.name else "New Item"
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title

        binding.editTextName.setText(currentItem.name)
        binding.editTextQuantity.setText(currentItem.quantity.toString())

        // Load groups and set up spinner
        loadGroups()

        binding.buttonSave.setOnClickListener {
            if (saveItemChanges()) {
                findNavController().navigateUp()
            }
        }

        // Handle back button press to save changes
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            saveItemChanges()
            findNavController().navigate(R.id.action_ItemFragment_to_ListFragment)
        }
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
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

    private fun saveItemChanges() : Boolean {
        val updatedName = binding.editTextName.text.toString().trim()
        val updatedQuantityText = binding.editTextQuantity.text.toString().trim()
        val updatedQuantity = updatedQuantityText.toIntOrNull() ?: 0

        if (updatedName.isEmpty()) {
            Toast.makeText(requireContext(), "Item name cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        currentItem.name = updatedName
        currentItem.quantity = updatedQuantity

        // Update the group based on spinner selection
        val selectedPosition = binding.spinnerGroup.selectedItemPosition
        currentItem.groupId = if (selectedPosition > 0 && selectedPosition - 1 < groups.size) {
            groups[selectedPosition - 1].id // -1 to account for "No Group" option
        } else {
            null // No group selected
        }

        // Pass the modified item back to the previous fragment
        val result = Bundle().apply {
            putSerializable("updatedItem", currentItem)
        }
        parentFragmentManager.setFragmentResult("itemUpdate", result)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
