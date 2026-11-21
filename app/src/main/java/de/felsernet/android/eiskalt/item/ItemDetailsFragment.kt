package de.felsernet.android.eiskalt.item

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseDetailsFragment
import de.felsernet.android.eiskalt.databinding.FragmentItemDetailsBinding
import de.felsernet.android.eiskalt.group.Group
import de.felsernet.android.eiskalt.group.GroupRepository
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

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
    override val newObjectTitleRes = R.string.item_new_title
    override val iconFilePrefix = "item_"

    private var groupAdapter: ArrayAdapter<String>? = null
    private lateinit var groups: List<Group>
    // the creation date shown and chosen in the ui, written to the item when saving
    private var createdAt: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getCurrentObject(): Item {
        // Use SafeArgs to get arguments
        val args = ItemDetailsFragmentArgs.fromBundle(requireArguments())
        return args.dataObject ?: Item("")
    }

    // the quantity is an editable number field, so prefill it with the plain digits the user types
    // and toIntOrNull() reads back, not with a locale specific number format
    @SuppressLint("SetTextI18n")
    override fun setupSpecificGuiElements(obj: Item) {
        // Set up UI
        binding.editTextQuantity.setText(obj.quantity.toString())

        createdAt = obj.createdAt
        showCreatedAt()
        binding.textViewCreatedAt.setOnClickListener { chooseCreatedAt() }

        // Load groups and set up spinner
        setupGroupSpinner(obj)
    }

    private fun setupGroupSpinner(currentItem: Item) {
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
                Toast.makeText(requireContext(), getString(R.string.item_groups_load_error, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreatedAt() {
        binding.textViewCreatedAt.text = createdAt?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(it) }
            ?: getString(R.string.item_created_not_set)
    }

    // choose the creation date, starting with the current one (or today if not set)
    private fun chooseCreatedAt() {
        val calendar = Calendar.getInstance()
        createdAt?.let { calendar.time = it }
        DatePickerDialog(requireContext(), { _, year, month, day ->
            calendar.set(year, month, day)
            createdAt = calendar.time
            showCreatedAt()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun getSpecificChanges(obj: Item) {
        val updatedQuantityText = binding.editTextQuantity.text.toString().trim()
        val updatedQuantity = updatedQuantityText.toIntOrNull() ?: 0
        obj.quantity = updatedQuantity
        obj.createdAt = createdAt

        // Update the group based on spinner selection
        val selectedPosition = binding.spinnerGroup.selectedItemPosition
        obj.groupId = if (selectedPosition > 0 && selectedPosition - 1 < groups.size) {
            groups[selectedPosition - 1].id // -1 to account for "No Group" option
        } else {
            null // No group selected
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
