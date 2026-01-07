package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestoreException
import de.felsernet.android.eiskalt.ListFragmentUtils.handleFirestoreException
import de.felsernet.android.eiskalt.ListFragmentUtils.setupAuthStateObserver
import de.felsernet.android.eiskalt.ListFragmentUtils.setupSwipeToDelete
import de.felsernet.android.eiskalt.databinding.FragmentInventoryListBinding
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the inventory list destination in the navigation.
 */
class InventoryListFragment : Fragment() {

    private var _binding: FragmentInventoryListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var adapter: MyAdapter
    private var items: MutableList<Item> = mutableListOf()
    private var isDataLoaded = false

    private lateinit var listName: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listName = arguments?.getString("listName") ?: "default"

        // Set the activity title to the list name
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = listName

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MyAdapter(items)
        binding.recyclerView.adapter = adapter

        setupAuthStateObserver {
            loadData()
        }

        // Listen for item updates from ItemFragment
        parentFragmentManager.setFragmentResultListener("itemUpdate", viewLifecycleOwner) { _, bundle ->
            val updatedItem = bundle.getSerializable("updatedItem") as? Item
            updatedItem?.let { updateItem(it) }
        }

        binding.fabAddItem.setOnClickListener {
            if (isDataLoaded) {
                findNavController().navigate(R.id.action_InventoryListFragment_to_ItemFragment)
            } else {
                Snackbar.make(binding.fabAddItem, "No data loaded", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(binding.fabAddItem).show()
            }
        }
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val repository = ListRepository()
                val fetchedItems = repository.getList(listName)
                items.clear()
                items.addAll(fetchedItems)
                adapter.notifyDataSetChanged()
                isDataLoaded = true

                // Add swipe-to-delete functionality using generalized helper
                // Only set up if binding is still available
                _binding?.let { binding ->
                    setupSwipeToDelete<Item>(
                        recyclerView = binding.recyclerView,
                        dataList = items,
                        adapter = adapter,
                        deleteMessage = "Item deleted",
                        deleteFunction = { item: Item ->
                            val itemRepository = ItemRepository(listName)
                            itemRepository.deleteItem(item)
                        }
                    )
                }
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(requireContext(), e, "load data")
            }
        }
    }

    private fun updateItem(updatedItem: Item) {
        val position = items.indexOfFirst { it.id == updatedItem.id }
        if (position != -1) {
            items[position] = updatedItem
            adapter.notifyItemChanged(position)
        } else {
            // Item not found, add as new item
            items.add(updatedItem)
            adapter.notifyItemInserted(items.size - 1)
        }
        lifecycleScope.launch {
            try {
                val itemRepository = ItemRepository(listName)
                itemRepository.saveItem(updatedItem)
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(requireContext(), e, "save data")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class MyAdapter(val items: MutableList<Item>) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.textView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.textView.text = item.name
            holder.itemView.setOnClickListener {
                val bundle = Bundle().apply {
                    putSerializable("item", item)
                }
                findNavController().navigate(R.id.action_InventoryListFragment_to_ItemFragment, bundle)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
