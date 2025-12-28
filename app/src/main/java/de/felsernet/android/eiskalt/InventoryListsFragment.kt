package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch
import de.felsernet.android.eiskalt.ListFragmentUtils.handleFirestoreException
import de.felsernet.android.eiskalt.ListFragmentUtils.setupAuthStateObserver
import de.felsernet.android.eiskalt.ListFragmentUtils.setupSwipeToDelete
import de.felsernet.android.eiskalt.databinding.FragmentInventoryListsBinding

/**
 * Fragment for displaying and managing inventory lists.
 */
class InventoryListsFragment : Fragment() {

    private var _binding: FragmentInventoryListsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ListsAdapter
    private var listNames: MutableList<String> = mutableListOf()
    private var isInitialLoad = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ListsAdapter(listNames)
        binding.recyclerView.adapter = adapter

        setupAuthStateObserver {
            loadLists()
        }

        binding.fabAddList.setOnClickListener {
            showCreateListDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        // Clear the saved list when user actively returns from the list dialog
        if (!isInitialLoad) {
            SharedPreferencesHelper.clearLastViewedList()
        }
    }

    private fun loadLists() {
        lifecycleScope.launch {
            try {
                val repository = InventoryRepository()
                val names = repository.getAllListNames()
                listNames.clear()
                listNames.addAll(names)
                adapter.notifyDataSetChanged()

                navigateToLastViewedListIfNeeded()

                // Add swipe-to-delete functionality using generalized helper
                // Post to ensure RecyclerView is fully laid out
                if (_binding != null) {
	                binding.recyclerView.post {
	                    if (_binding == null) return@post
	                    setupSwipeToDelete(
	                        recyclerView = binding.recyclerView,
	                        dataList = listNames,
	                        adapter = adapter,
	                        deleteMessage = "List deleted"
	                    ) { listName: String ->
	                        lifecycleScope.launch {
	                            val repository = InventoryRepository()
	                            repository.deleteList(listName)
	                        }
	                    }
	                }
				}
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(requireContext(), e, "load data")
            }
        }
    }

    private fun navigateToLastViewedListIfNeeded() {
        // Navigate to last viewed list if it's the initial load
        if (isInitialLoad) {
            val lastList = SharedPreferencesHelper.getLastViewedList()
            if (lastList != null && listNames.contains(lastList)) {
                val bundle = Bundle().apply {
                    putString("listName", lastList)
                }
                findNavController().navigate(R.id.action_InventoryListsFragment_to_InventoryListFragment, bundle)
            }
            isInitialLoad = false
        }
    }

    private fun showCreateListDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_list, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editTextListName)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Create New List")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val listName = editText.text.toString().trim()
                if (listName.isNotBlank()) {
                    createList(listName)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        editText.addTextChangedListener {
            val isEnabled = it.toString().trim().isNotBlank()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }

        dialog.show()
    }

    private fun createList(listName: String) {
        lifecycleScope.launch {
            try {
                val repository = InventoryRepository()
                repository.createList(listName)
                listNames.add(listName)
                adapter.notifyItemInserted(listNames.size - 1)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to create list", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class ListsAdapter(val listNames: MutableList<String>) :
        RecyclerView.Adapter<ListsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val listName = listNames[position]
            holder.textView.text = listName
            holder.itemView.setOnClickListener {
                // Save the last viewed list
                SharedPreferencesHelper.saveLastViewedList(listName)
                val bundle = Bundle().apply {
                    putString("listName", listName)
                }
                findNavController().navigate(R.id.action_InventoryListsFragment_to_InventoryListFragment, bundle)
            }
        }

        override fun getItemCount(): Int = listNames.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.textView)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
