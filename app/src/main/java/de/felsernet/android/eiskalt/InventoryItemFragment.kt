package de.felsernet.android.eiskalt

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import de.felsernet.android.eiskalt.databinding.FragmentInventoryItemBinding
import java.io.Serializable

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
            binding.textViewName.text = "Name: ${item.name}"
            binding.textViewId.text = "ID: ${item.id}"
            binding.textViewQuantity.text = "Quantity: ${item.quantity}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
