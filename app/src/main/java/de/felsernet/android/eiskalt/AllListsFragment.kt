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
import androidx.lifecycle.LifecycleCoroutineScope
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
import de.felsernet.android.eiskalt.databinding.FragmentAllListsBinding

/**
 * Fragment for displaying and managing all lists.
 */
class AllListsFragment : Fragment() {

    private var _binding: FragmentAllListsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ListsAdapter
    private var listInfos: MutableList<ListInfo> = mutableListOf()
    private var isInitialLoad = true
    private var hasLoadedLists = false

    data class ListInfo(val name: String, val itemCount: Int)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set custom title if available
        updateTitle()

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ListsAdapter(listInfos)
        binding.recyclerView.adapter = adapter

        setupAuthStateObserver {
            if (!hasLoadedLists) {
                loadLists()
                hasLoadedLists = true
            }
        }

        binding.fabAddList.setOnClickListener {
            showCreateListDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        // Clear the saved list when user actively returns from the list dialog
        if (!isInitialLoad) {
            val lastViewedList = SharedPreferencesHelper.getLastViewedList()
            SharedPreferencesHelper.clearLastViewedList()
            // Refresh item count only for the list we returned from
            if (lastViewedList != null) {
                refreshListCount(lastViewedList)
            }
        }
        // Update title in case it was changed in settings
        updateTitle()

        // Ensure swipe-to-delete functionality is set up when returning from a list
        setupSwipeToDeleteFunctionality()
    }

    private fun updateTitle() {
        val customTitle = SharedPreferencesHelper.getCustomTitle()
        val title = customTitle ?: getString(R.string.all_lists_fragment_default_label)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title
    }

    private fun loadLists() {
        lifecycleScope.launch {
            try {
                val names = ListRepository().getAll()

                // Fetch item counts for each list
                listInfos.clear()
                listInfos.addAll(names.map { listName ->
                    val itemCount = ItemRepository(listName).count()
                    ListInfo(listName, itemCount)
                })
                adapter.notifyDataSetChanged()

                navigateToLastViewedListIfNeeded()
                setupSwipeToDeleteFunctionality()
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(requireContext(), e, "load data")
            }
        }
    }

    private fun refreshListCount(listName: String) {
        lifecycleScope.launch {
            try {
                // Find the index of the list to update
                val index = listInfos.indexOfFirst { it.name == listName }
                if (index != -1) {
                    val newCount = ItemRepository(listName).count()
                    if (listInfos[index].itemCount != newCount) {
                        // ListInfo is a data class with immutable properties, so use copy() to create updated instance
                        listInfos[index] = listInfos[index].copy(itemCount = newCount)
                        adapter.notifyItemChanged(index)
                    }
                }
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(requireContext(), e, "refresh data")
            }
        }
    }

    /**
     * Sets up swipe-to-delete functionality for the lists
     * This needs to be called whenever the fragment becomes visible to ensure
     * the swipe functionality works properly after navigation
     */
    private fun setupSwipeToDeleteFunctionality() {
        // Add swipe-to-delete functionality using generalized helper
        // Post to ensure RecyclerView is fully laid out
        if (_binding != null) {
            binding.recyclerView.post {
                if (_binding == null) return@post
                setupSwipeToDelete<ListInfo>(
                    recyclerView = binding.recyclerView,
                    dataList = listInfos,
                    adapter = adapter,
                    deleteMessage = "List deleted",
                    deleteFunction = { listInfo: ListInfo ->
                        val listRepository = ListRepository()
                        listRepository.delete(listInfo.name)
                    }
                )
            }
        }
    }

    private fun navigateToLastViewedListIfNeeded() {
        // Navigate to last viewed list if it's the initial load
        if (isInitialLoad) {
            val lastList = SharedPreferencesHelper.getLastViewedList()
            if (lastList != null && listInfos.any { it.name == lastList }) {
                val bundle = Bundle().apply {
                    putString("listName", lastList)
                }
                findNavController().navigate(R.id.action_AllListsFragment_to_ListFragment, bundle)
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
                val listRepository = ListRepository()
                listRepository.save(listName)
                listInfos.add(ListInfo(listName, 0))
                adapter.notifyItemInserted(listInfos.size - 1)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to create list", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class ListsAdapter(val listInfos: MutableList<ListInfo>) :
        RecyclerView.Adapter<ListsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val listInfo = listInfos[position]
            holder.textView.text = listInfo.name
            holder.textViewItemCount.text = "${listInfo.itemCount}"
            holder.itemView.setOnClickListener {
                // Save the last viewed list
                SharedPreferencesHelper.saveLastViewedList(listInfo.name)
                val bundle = Bundle().apply {
                    putString("listName", listInfo.name)
                }
                findNavController().navigate(R.id.action_AllListsFragment_to_ListFragment, bundle)
            }
        }

        override fun getItemCount(): Int = listInfos.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.textView)
            val textViewItemCount: TextView = itemView.findViewById(R.id.textViewItemCount)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
