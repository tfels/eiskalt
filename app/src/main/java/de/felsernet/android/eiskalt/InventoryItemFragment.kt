package de.felsernet.android.eiskalt

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        if (item != null) {
            binding.editTextName.setText(item.name)
            binding.textViewId.text = item.id.toString()
            binding.editTextQuantity.setText(item.quantity.toString())
        }

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
        val item = arguments?.getSerializable("inventoryItem") as? InventoryItem
        if (item != null) {
            val updatedName = binding.editTextName.text.toString().trim()
            val updatedQuantityText = binding.editTextQuantity.text.toString().trim()
            val updatedQuantity = updatedQuantityText.toIntOrNull() ?: item.quantity

            if (updatedName.isNotEmpty()) {
                val updatedItem = item.copy(name = updatedName, quantity = updatedQuantity)

                // Pass the updated item back to the previous fragment
                val result = Bundle().apply {
                    putSerializable("updatedInventoryItem", updatedItem)
                }
                parentFragmentManager.setFragmentResult("itemUpdate", result)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
