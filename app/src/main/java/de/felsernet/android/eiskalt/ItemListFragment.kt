package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import de.felsernet.android.eiskalt.databinding.FragmentItemListBinding

/**
 * Fragment displaying the list of items in a shopping list.
 * Uses ViewModel with Flows for reactive data management.
 */
class ItemListFragment : BaseListFragment<Item>() {

    private var _binding: FragmentItemListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override val deleteMessage: String = "Item deleted"
    override val adapterLayoutId: Int = R.layout.item_row
    override val adapterViewHolderFactory = ::ItemViewHolder

    // Shared ViewModel for items (survives fragment recreation)
    override val viewModel: ItemViewModel by activityViewModels()

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
        setupTitle(listInfo)
    }

    private fun setupTitle(listInfo: ListInfo) {
        val actionBar = (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar
        if (actionBar == null)
            return

        actionBar.title = listInfo.name

        // Set icon for the title bar if available
        listInfo.icon?.let { iconInfo ->
            // Load icon drawable directly using IconUtils
            IconUtils.loadIconDrawableAsync(requireContext(), iconInfo) { drawable ->
                drawable?.let {
                    actionBar.setDisplayShowHomeEnabled(true)
                    actionBar.setDisplayUseLogoEnabled(true)
                    actionBar.setIcon(it)
                }
            }
        }

        // Clear any existing icon if no icon is set
        if (listInfo.icon == null) {
            actionBar.setDisplayShowHomeEnabled(false)
            actionBar.setDisplayUseLogoEnabled(false)
            actionBar.setIcon(null)
        }
    }

    override fun initializeViewModel() {
        // Use SafeArgs to get the listName argument
        val args = ItemListFragmentArgs.fromBundle(requireArguments())
        val listInfo = args.listInfo
        
        // Call the ItemViewModel-specific initialize method with ListInfo
        viewModel.initialize(sharedMessageViewModel, listInfo)
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
