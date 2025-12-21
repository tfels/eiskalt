package de.felsernet.android.eiskalt

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import de.felsernet.android.eiskalt.databinding.FragmentInventoryItemBinding

/**
 * A simple [Fragment] subclass as the inventory item destination in the navigation.
 */
class InventoryItemFragment : Fragment() {

    private var _binding: FragmentInventoryItemBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var currentItem: InventoryItem

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
		if(item == null)
        {
            Toast.makeText(requireContext(), "Item not found for update", Toast.LENGTH_SHORT).show()
            return
        }
        currentItem = item

        binding.editTextName.setText(currentItem.name)
        binding.textViewId.text = currentItem.id.toString()
        binding.editTextQuantity.setText(currentItem.quantity.toString())

        binding.buttonSave.setOnClickListener {
            saveItemChanges()
            findNavController().navigateUp()
        }

        // Handle back button press to save changes
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            saveItemChanges()
            findNavController().navigateUp()
        }
    }

    private fun saveItemChanges() {
        val updatedName = binding.editTextName.text.toString().trim()
        val updatedQuantityText = binding.editTextQuantity.text.toString().trim()
        val updatedQuantity = updatedQuantityText.toIntOrNull() ?: 0

        if (updatedName.isNotEmpty()) {
            currentItem.name = updatedName
            currentItem.quantity = updatedQuantity

            // Pass the modified item back to the previous fragment
            val result = Bundle().apply {
                putSerializable("updatedInventoryItem", currentItem)
            }
            parentFragmentManager.setFragmentResult("itemUpdate", result)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
