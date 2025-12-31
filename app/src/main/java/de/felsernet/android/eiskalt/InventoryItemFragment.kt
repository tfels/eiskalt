package de.felsernet.android.eiskalt

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.felsernet.android.eiskalt.databinding.FragmentInventoryItemBinding
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the inventory item destination in the navigation.
 */
class InventoryItemFragment : Fragment() {

    private var _binding: FragmentInventoryItemBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var currentItem: InventoryItem
    private val groupRepository = GroupRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentInventoryItemBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val item = arguments?.getSerializable("inventoryItem") as? InventoryItem
        currentItem = item ?: InventoryItem("", quantity = 0)

        // Set the title
        val title = if (currentItem.name.isNotEmpty()) currentItem.name else "New Item"
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title

        binding.editTextName.setText(currentItem.name)
        binding.textViewId.text = currentItem.id.toString()
        binding.editTextQuantity.setText(currentItem.quantity.toString())

        // Load and display group information
        loadGroupInfo()

        binding.buttonSave.setOnClickListener {
            if (saveItemChanges()) {
                findNavController().navigateUp()
            }
        }

        // Handle back button press to save changes
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            saveItemChanges()
            findNavController().navigateUp()
        }
    }

    private fun loadGroupInfo() {
        if (currentItem.groupId > 0) {
            lifecycleScope.launch {
                try {
                    val group = groupRepository.getGroupById(currentItem.groupId)
                    binding.textViewGroup.text = group?.name ?: getString(R.string.no_group)
                } catch (e: Exception) {
                    binding.textViewGroup.text = getString(R.string.no_group)
                }
            }
        } else {
            binding.textViewGroup.text = getString(R.string.no_group)
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

        // Pass the modified item back to the previous fragment
        val result = Bundle().apply {
            putSerializable("updatedInventoryItem", currentItem)
        }
        parentFragmentManager.setFragmentResult("itemUpdate", result)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
