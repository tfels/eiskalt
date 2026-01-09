package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch
import de.felsernet.android.eiskalt.databinding.FragmentAllListsBinding

/**
 * Fragment for displaying and managing all lists.
 */
class AllListsFragment : BaseListFragment<ListInfo>() {

    private var _binding: FragmentAllListsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AllListsAdapter
    private var listInfos: MutableList<ListInfo> = mutableListOf()
    private var isInitialLoad = true

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
        adapter = AllListsAdapter(listInfos) { listInfo ->
            // Save the last viewed list
            SharedPreferencesHelper.saveLastViewedList(listInfo.name)
            val bundle = Bundle().apply {
                putString("listName", listInfo.name)
            }
            findNavController().navigate(R.id.action_AllListsFragment_to_ListFragment, bundle)
        }
        binding.recyclerView.adapter = adapter
        binding.fabAddList.setOnClickListener {
            showCreateListDialog()
        }

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
    }

    private fun updateTitle() {
        val customTitle = SharedPreferencesHelper.getCustomTitle()
        val title = customTitle ?: getString(R.string.all_lists_fragment_default_label)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title
    }

    override fun loadData() {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

