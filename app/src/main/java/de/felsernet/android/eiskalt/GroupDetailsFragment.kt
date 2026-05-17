package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup items list
        val itemsAdapter = GenericListAdapter<Item, ItemViewHolder>(
            R.layout.item_row,
            { itemView -> ItemViewHolder(itemView) { /* no click action needed here */ } }
        )
        binding.recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = itemsAdapter
        }

        // Load items for this group
        val currentGroup = getCurrentObject()
        if (currentGroup.id.isNotEmpty()) {
            viewModel.loadItemsForGroup(currentGroup.id)
        }

        // Observe items in group
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.itemsInGroup.collect { items ->
                    itemsAdapter.submitList(items.map { DisplayItem.Content(it) })
                    
                    binding.textViewItemsLabel.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                    binding.recyclerViewItems.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun getCurrentObject(): Group {
        // Use SafeArgs to get the group argument (nullable for new groups)
        val args = GroupDetailsFragmentArgs.fromBundle(requireArguments())
        return args.dataObject ?: Group("")
    }

    override fun setupSpecificGuiElements(obj: Group) {
        // Set up UI
    }

    override fun getSpecificChanges(obj: Group) {
        // Update the group
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
