package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import de.felsernet.android.eiskalt.databinding.FragmentListBinding
import kotlinx.coroutines.launch

/**
 * Fragment displaying the list of items in a shopping list.
 * Uses ViewModel with Flows for reactive data management.
 */
class ListFragment : BaseListFragment<Item>() {

    private var _binding: FragmentListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override val recyclerView: RecyclerView get() = binding.recyclerView
    override val fabView: View get() = binding.fabAddItem
    override val deleteMessage: String = "Item deleted"
    override val adapterLayoutId: Int = R.layout.item_row
    override val adapterViewHolderFactory = ::ItemViewHolder

    // Shared ViewModel for items (survives fragment recreation)
    private val viewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use SafeArgs to get the listName argument
        val args = ListFragmentArgs.fromBundle(requireArguments())
        val listName = args.listName

        // Set list name in ViewModel and load items
        viewModel.initialize(sharedMessageViewModel, listName)

        // Set the activity title to the list name
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = listName

        // Collect items from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect {
                    objectsList.clear()
                    objectsList.addAll(it)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun loadData() {
        // Data is loaded via ViewModel's Flow - no need for manual load
        viewModel.loadItems()
    }

    override fun onClickAdd() {
        // Pass null item for new item creation
        val action = ListFragmentDirections.actionListFragmentToItemFragment(null)
        findNavController().navigate(action)
    }

    override suspend fun onSwipeDelete(item: Item) {
        viewModel.deleteItem(item)
    }

    override fun onClickObject(item: Item) {
        val action = ListFragmentDirections.actionListFragmentToItemFragment(item)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
