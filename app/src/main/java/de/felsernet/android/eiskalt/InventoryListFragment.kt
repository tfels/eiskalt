package de.felsernet.android.eiskalt

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestoreException
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.felsernet.android.eiskalt.ListFragmentUtils.handleFirestoreException
import de.felsernet.android.eiskalt.ListFragmentUtils.setupAuthStateObserver
import de.felsernet.android.eiskalt.BaseSwipeToDeleteCallback
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.databinding.FragmentInventoryListBinding

/**
 * A simple [Fragment] subclass as the inventory list destination in the navigation.
 */
class InventoryListFragment : Fragment() {

    private var _binding: FragmentInventoryListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var adapter: MyAdapter
    private var items: MutableList<InventoryItem>? = null
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

        setupAuthStateObserver {
            loadData()
        }

        // Listen for item updates from InventoryItemFragment
        parentFragmentManager.setFragmentResultListener("itemUpdate", viewLifecycleOwner) { _, bundle ->
            val updatedItem = bundle.getSerializable("updatedInventoryItem") as? InventoryItem
            updatedItem?.let { updateItem(it) }
        }

        binding.fabAddItem.setOnClickListener {
            if (isDataLoaded) {
                findNavController().navigate(R.id.action_InventoryListFragment_to_InventoryItemFragment)
            } else {
                com.google.android.material.snackbar.Snackbar.make(binding.fabAddItem, "No data loaded", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(binding.fabAddItem).show()
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val repository = InventoryRepository()
                val fetchedItems = repository.getList(listName)
                InventoryItem.initializeCounter(fetchedItems)
                items = fetchedItems.toMutableList()
                isDataLoaded = true
                adapter = MyAdapter(items!!)
                binding.recyclerView.adapter = adapter

                val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(adapter))
                itemTouchHelper.attachToRecyclerView(binding.recyclerView)
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(requireContext(), e, "load data")
            }
        }
    }

    private fun updateItem(updatedItem: InventoryItem) {
        val position = items!!.indexOfFirst { it.id == updatedItem.id }
        if (position != -1) {
            items!![position] = updatedItem
            adapter.notifyItemChanged(position)
        } else {
            // Item not found, add as new item
            items!!.add(updatedItem)
            adapter.notifyItemInserted(items!!.size - 1)
        }
        lifecycleScope.launch {
            try {
                val repository = InventoryRepository()
                repository.saveList(listName, items!!)
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(requireContext(), e, "save data")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    inner class MyAdapter(val items: MutableList<InventoryItem>) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

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
                    putSerializable("inventoryItem", item)
                }
                findNavController().navigate(R.id.action_InventoryListFragment_to_InventoryItemFragment, bundle)
            }
        }

        override fun getItemCount(): Int = items.size


    }

    inner class SwipeToDeleteCallback(private val adapter: MyAdapter) : BaseSwipeToDeleteCallback() {

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val itemToDelete = items!![position]

            lifecycleScope.launch {
                try {
                    val repository = InventoryRepository()
                    repository.deleteItem(listName, itemToDelete)
                    items!!.removeAt(position)
                    adapter.notifyItemRemoved(position)
                } catch (e: FirebaseFirestoreException) {
                    // Restore the item to the list since delete failed
                    items!!.add(position, itemToDelete)
                    adapter.notifyItemInserted(position)
                    handleFirestoreException(requireContext(), e, "delete item")
                }
            }
        }
    }
}
