package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
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

    private var isDataLoaded = false

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

        listName = arguments?.getString("listName") ?: "default"

        // Set the activity title to the list name
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = listName

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ListAdapter(objectsList) { item ->
            val bundle = Bundle().apply {
                putSerializable("item", item)
            }
            findNavController().navigate(R.id.action_ListFragment_to_ItemFragment, bundle)
        }
        binding.recyclerView.adapter = adapter

        // Listen for item updates from ItemFragment
        parentFragmentManager.setFragmentResultListener("itemUpdate", viewLifecycleOwner) { _, bundle ->
            val updatedItem = bundle.getSerializable("updatedItem") as? Item
            updatedItem?.let { updateItem(it) }
        }

        binding.fabAddItem.setOnClickListener {
            if (isDataLoaded) {
                findNavController().navigate(R.id.action_ListFragment_to_ItemFragment)
            } else {
                Snackbar.make(binding.fabAddItem, "No data loaded", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(binding.fabAddItem).show()
            }
        }

        setupSwipeToDelete(
            recyclerView = binding.recyclerView,
            deleteMessage = "Item deleted",
            deleteFunction = { item: Item ->
                ItemRepository(listName).delete(item.id)
            }
        )
    }

    override fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val fetchedItems = ItemRepository(listName).getAll()
                objectsList.clear()
                objectsList.addAll(fetchedItems)
                adapter.notifyDataSetChanged()
                isDataLoaded = true
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(requireContext(), e, "load data")
            }
        }
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
                handleFirestoreException(requireContext(), e, "save data")
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
