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
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch
import de.felsernet.android.eiskalt.databinding.FragmentAllListsBinding
import kotlin.getValue

/**
 * Fragment for displaying and managing all lists.
 */
class AllListsFragment : BaseListFragment<ListInfo>() {

    private var _binding: FragmentAllListsBinding? = null
    private val binding get() = _binding!!

    override val deleteMessage: String = "List deleted"
    override val adapterLayoutId: Int = R.layout.list_row
    override val adapterViewHolderFactory = ::ListViewHolder

    // Shared ViewModel for groups (survives fragment recreation)
    override val viewModel: ListViewModel by activityViewModels()

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

        // Collect list flow in repeatOnLifecycle block
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.list.collect { listInfos ->
                        objectsList.clear()
                        objectsList.addAll(listInfos)
                        adapter.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.dataLoaded.collect {
                        navigateToLastViewedListIfNeeded()
                    }
                }
            }
        }
    }

    /**
     * Handle long-press on a list item to edit it
     */
    override fun onLongClickObject(listInfo: ListInfo): Boolean {
        // Navigate to ListDetailsFragment for editing the existing list
        val action = AllListsFragmentDirections.actionAllListsFragmentToListDetailsFragment(listInfo)
        findNavController().navigate(action)
        return true // Consume the long-press event
    }

    override fun onResume() {
        super.onResume()
        // Clear the saved list when user actively returns from the list dialog
        if (!isInitialLoad) {
            val lastViewedListId = SharedPreferencesHelper.getLastViewedList()
            SharedPreferencesHelper.clearLastViewedList()
            // Refresh item count only for the list we returned from
            val lastViewedList = lastViewedListId?.let { id -> objectsList.firstOrNull { it.id == id } }
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

    override fun onClickAdd() {
        // Navigate to ListDetailsFragment for creating a new list
        val action = AllListsFragmentDirections.actionAllListsFragmentToListDetailsFragment(null)
        findNavController().navigate(action)
    }

    override fun onClickObject(listInfo: ListInfo) {
        // Save the last viewed list
        SharedPreferencesHelper.saveLastViewedList(listInfo.id)
        // Use SafeArgs for navigation
        val action = AllListsFragmentDirections.actionAllListsFragmentToItemListFragment(listInfo)
        findNavController().navigate(action)
    }


    private fun refreshListCount(listInfo: ListInfo) {
        lifecycleScope.launch {
            try {
                // Find the index of the list to update
                val index = objectsList.indexOfFirst { it.id == listInfo.id }
                if (index != -1) {
                    val newCount = ItemRepository(listInfo).count()
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
            
            val lastListId = SharedPreferencesHelper.getLastViewedList()
            val lastList = lastListId?.let { id -> objectsList.firstOrNull { it.id == id } }
            if (lastList != null) {
                // Use SafeArgs for navigation
                val action = AllListsFragmentDirections.actionAllListsFragmentToItemListFragment(lastList)
                navController.navigate(action)
            }
            isInitialLoad = false
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
