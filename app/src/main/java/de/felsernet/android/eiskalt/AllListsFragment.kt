package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
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

    private var isInitialLoad = true

    override val recyclerView: RecyclerView get() = binding.recyclerView
    override val fabView: View get() = binding.fabAddList
    override val deleteMessage: String = "List deleted"
    override val adapterLayoutId: Int = R.layout.list_row
    override val adapterViewHolderFactory = ::ListViewHolder

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
                // Use the new method that directly returns ListInfo objects
                val listInfoList = ListRepository().getAllListInfo()
                objectsList.clear()
                objectsList.addAll(listInfoList.sortedBy { it.name.lowercase() })
                adapter.notifyDataSetChanged()

                navigateToLastViewedListIfNeeded()
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(e, "load data")
            }
        }
    }

    override fun onClickAdd() {
        showCreateListDialog()
    }

    override suspend fun onSwipeDelete(listInfo: ListInfo) {
        val listRepository = ListRepository()
        listRepository.delete(listInfo.name)
    }

    override fun onClickObject(listInfo: ListInfo) {
        // Save the last viewed list
        SharedPreferencesHelper.saveLastViewedList(listInfo.name)
        // Use SafeArgs for navigation
        val action = AllListsFragmentDirections.actionAllListsFragmentToItemListFragment(listInfo.name)
        findNavController().navigate(action)
    }


    private fun refreshListCount(listName: String) {
        lifecycleScope.launch {
            try {
                // Find the index of the list to update
                val index = objectsList.indexOfFirst { it.name == listName }
                if (index != -1) {
                    val newCount = ItemRepository(listName).count()
                    if (objectsList[index].itemCount != newCount) {
                        // ListInfo is a data class with immutable properties, so use copy() to create updated instance
                        objectsList[index] = objectsList[index].copy(itemCount = newCount)
                        adapter.notifyItemChanged(index)
                    }
                }
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreException(e, "refresh data")
            }
        }
    }

    private fun navigateToLastViewedListIfNeeded() {
        // Navigate to last viewed list if it's the initial load
        if (isInitialLoad) {
            // Check if this fragment is still the current destination before navigating
            val navController = findNavController()
            val currentDestination = navController.currentDestination
            if (currentDestination?.id != R.id.AllListsFragment) {
                isInitialLoad = false
                return
            }
            
            val lastList = SharedPreferencesHelper.getLastViewedList()
            if (lastList != null && objectsList.any { it.name == lastList }) {
                // Use SafeArgs for navigation
                val action = AllListsFragmentDirections.actionAllListsFragmentToItemListFragment(lastList)
                navController.navigate(action)
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
                objectsList.add(ListInfo(listName, "", 0))
                adapter.notifyItemInserted(objectsList.size - 1)
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
