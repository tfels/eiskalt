package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import de.felsernet.android.eiskalt.databinding.FragmentItemListBinding
import kotlinx.coroutines.launch

/**
 * Fragment displaying the list of items in a shopping list.
 * Uses ViewModel with Flows for reactive data management.
 * Items are grouped by their groupId property.
 */
class ItemListFragment : BaseListFragment<Item>() {

    private var _binding: FragmentItemListBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    override val deleteMessage: String = "Item deleted"
    override val adapterLayoutId: Int = R.layout.item_row
    override val adapterViewHolderFactory = ::ItemViewHolder

    // Shared ViewModel for items (survives fragment recreation)
    override val viewModel: ItemViewModel by activityViewModels()

    // Cached group names: groupId -> groupName
    private var groupNameMap: Map<String, String> = emptyMap()

    // Typed accessor for our grouped adapter
    private val itemListAdapter: ItemListAdapter
        get() = adapter as ItemListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use SafeArgs to get the listName argument
        val args = ItemListFragmentArgs.fromBundle(requireArguments())
        val listInfo = args.listInfo

        // Set the activity title to the list name
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = listInfo.name

        // Load group names for displaying section headers
        loadGroupNames()
    }

    override fun initializeViewModel() {
        // Use SafeArgs to get the listName argument
        val args = ItemListFragmentArgs.fromBundle(requireArguments())
        val listInfo = args.listInfo
        
        // Call the ItemViewModel-specific initialize method with ListInfo
        viewModel.initialize(sharedMessageViewModel, listInfo)
    }

    /**
     * Load group names from the GroupRepository to use in section headers.
     */
    private fun loadGroupNames() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val groups = GroupRepository.getInstance().getAll()
                groupNameMap = groups.associate { it.id to it.name }
                // If data already loaded, refresh the display with group names
                if (objectsList.isNotEmpty()) {
                    rebuildDisplayItems()
                }
            } catch (e: Exception) {
                // Silently ignore; group names will show as "Unknown"
            }
        }
    }

    override fun createAdapter(): RecyclerView.Adapter<*> {
        return ItemListAdapter(
            sourceList = objectsList,
            groupNameMapProvider = { groupNameMap },
            onClick = ::onClickObject,
            onLongClick = ::onLongClickObject
        )
    }

    override fun rebuildDisplayItems(): Boolean {
        itemListAdapter.refreshDisplayList()
        adapter.notifyDataSetChanged()
        return true
    }

    // --- Swipe-to-delete hooks for grouped list ---

    override fun canSwipeItemAt(position: Int): Boolean {
        return itemListAdapter.getDisplayItem(position) is ListItem.ItemRow
    }

    override fun getSwipedItem(position: Int): Item {
        return (itemListAdapter.getDisplayItem(position) as ListItem.ItemRow).item
    }

    override fun removeSwipedItem(position: Int): Item {
        val listItem = itemListAdapter.removeDisplayItem(position)
        val item = (listItem as ListItem.ItemRow).item
        adapter.notifyItemRemoved(position)
        objectsList.remove(item)
        return item
    }

    override fun restoreSwipedItem(item: Item, originalPosition: Int) {
        objectsList.add(item)
        objectsList.sortBy { it.name.lowercase() }
        rebuildDisplayItems()
    }

    override fun onClickAdd() {
        // Pass null item for new item creation
        val action = ItemListFragmentDirections.actionItemListFragmentToItemDetailsFragment(null)
        findNavController().navigate(action)
    }

    override fun onClickObject(item: Item) {
        val action = ItemListFragmentDirections.actionItemListFragmentToItemDetailsFragment(item)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
