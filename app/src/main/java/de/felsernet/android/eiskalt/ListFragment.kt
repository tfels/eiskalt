package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestoreException
import de.felsernet.android.eiskalt.databinding.FragmentListBinding
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the list destination in the navigation.
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
    private lateinit var listName: String

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
        listName = args.listName

        // Set the activity title to the list name
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = listName

        // Listen for item updates from ItemFragment
        parentFragmentManager.setFragmentResultListener("itemUpdate", viewLifecycleOwner) { _, bundle ->
            val updatedItem = bundle.getSerializable("updatedItem") as? Item
            updatedItem?.let { updateItem(it) }
        }
    }

    override fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val fetchedItems = ItemRepository(listName).getAll()
                objectsList.clear()
                objectsList.addAll(fetchedItems.sortedBy { it.name.lowercase() })
                adapter.notifyDataSetChanged()
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(e, "load data")
            }
        }
    }

    override fun onClickAdd() {
        // Pass null item for new item creation
        val action = ListFragmentDirections.actionListFragmentToItemFragment(null)
        findNavController().navigate(action)
    }

    override suspend fun onSwipeDelete(item: Item) {
        ItemRepository(listName).delete(item.id)
    }

    override fun onClickObject(item: Item) {
        // Use SafeArgs for navigation
        val action = ListFragmentDirections.actionListFragmentToItemFragment(item)
        findNavController().navigate(action)
    }

    private fun updateItem(updatedItem: Item) {
        val position = objectsList.indexOfFirst { it.id == updatedItem.id }
        if (position != -1) {
            objectsList[position] = updatedItem
            adapter.notifyItemChanged(position)
        } else {
            // Item not found, add as new item
            objectsList.add(updatedItem)
            adapter.notifyItemInserted(objectsList.size - 1)
        }
        lifecycleScope.launch {
            try {
                ItemRepository(listName).save(updatedItem)
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(e, "save data")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
