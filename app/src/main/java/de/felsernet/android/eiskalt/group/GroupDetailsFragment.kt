package de.felsernet.android.eiskalt.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseDetailsFragment
import de.felsernet.android.eiskalt.databinding.FragmentGroupDetailsBinding
import kotlinx.coroutines.launch

/**
 * Fragment for managing groups (add/edit) similar to ItemDetailsFragment
 * Uses ViewModel with Flows for state management and data sharing.
 */
class GroupDetailsFragment : BaseDetailsFragment<Group>() {

    private var _binding: FragmentGroupDetailsBinding? = null
    private val binding get() = _binding!!
    // Shared ViewModel survives fragment recreation
    override val viewModel: GroupViewModel by activityViewModels()
    override val newObjectTitle = "New Group"
    override val iconFilePrefix = "group_"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getCurrentObject(): Group {
        // Use SafeArgs to get the group argument (nullable for new groups)
        val args = GroupDetailsFragmentArgs.fromBundle(requireArguments())
        return args.dataObject ?: Group("")
    }

    override fun setupSpecificGuiElements(obj: Group) {
        showItemsUsingGroup(obj)
    }

    /**
     * Show how many items (and which ones) use this group
     */
    private fun showItemsUsingGroup(group: Group) {
        // a new group cannot be used by any item yet
        if (group.id.isEmpty()) {
            binding.textViewUsageCount.visibility = View.GONE
            return
        }

        binding.textViewUsageCount.setText(R.string.group_usage_loading)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // get all items using this group
                val usages = viewModel.getItemsUsingGroup(group.id)
                    .sortedWith(compareBy({ it.listInfo.name.lowercase() }, { it.item.name.lowercase() }))

                // set count text
                binding.textViewUsageCount.text = if (usages.isEmpty()) {
                    getString(R.string.group_usage_none)
                } else {
                    resources.getQuantityString(R.plurals.group_usage_count, usages.size, usages.size)
                }
                // setup list of item names, with the list name as caption
                binding.textViewUsageItems.text = usages
                    .groupBy { it.listInfo.name }
                    .map { (listName, listUsages) ->
                        listName + "\n" + listUsages.joinToString("\n") {
                            getString(R.string.group_usage_entry, it.item.name)
                        }
                    }
                    .joinToString("\n")
                binding.textViewUsageItems.visibility = if (usages.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                binding.textViewUsageCount.setText(R.string.group_usage_error)
                sharedMessageViewModel.showErrorMessage("db", "Error loading items of group \"${group.name}\": ${e.message}", e)
            }
        }
    }

    override fun getSpecificChanges(obj: Group) {
        // Update the group
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
