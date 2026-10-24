package de.felsernet.android.eiskalt.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseListFragment
import de.felsernet.android.eiskalt.databinding.FragmentAllListsBinding
import de.felsernet.android.eiskalt.item.ItemRepository
import de.felsernet.android.eiskalt.settings.UserSettingsRepository
import kotlinx.coroutines.launch

/**
 * Fragment for displaying and managing all lists.
 */
class AllListsFragment : BaseListFragment<ListInfo>() {

    private var _binding: FragmentAllListsBinding? = null
    private val binding get() = _binding!!

    override val deleteMessage: String = "List deleted"
    override val adapterLayoutId: Int = R.layout.list_row
    override val adapterViewHolderFactory = { view: View ->
        ListViewHolder(view, ::onClickObject, ::onLongClickObject)
    }

    // Shared ViewModel for groups (survives fragment recreation)
    override val viewModel: ListViewModel by activityViewModels()

    private var isInitialLoad = true

    // Sets the (custom) title right when this fragment becomes the current destination again,
    // so no other title is shown in between (the nav graph has no label for this fragment).
    // Registered for the whole fragment lifetime: the fragment stays alive while a list is shown.
    private val titleListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        if (destination.id == R.id.AllListsFragment)
            showTitle(UserSettingsRepository.getCachedSettings().customTitle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findNavController().addOnDestinationChangedListener(titleListener)
    }

    override fun onDestroy() {
        findNavController().removeOnDestinationChangedListener(titleListener)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Collect list flow in repeatOnLifecycle block
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dataLoaded.collect {
                    navigateToLastViewedListIfNeeded()
                }
            }
        }
    }

    /**
     * Handle long-press on a list item to edit it
     */
    fun onLongClickObject(listInfo: ListInfo): Boolean {
        // Navigate to ListDetailsFragment for editing the existing list
        val action = AllListsFragmentDirections.actionAllListsFragmentToListDetailsFragment(listInfo)
        findNavController().navigate(action)
        return true // Consume the long-press event
    }

    override fun onResume() {
        super.onResume()
        // Clear the saved list when user actively returns from the list dialog
        if (!isInitialLoad) {
            val lastViewedListId = UserSettingsRepository.getLastViewedList()
            UserSettingsRepository.saveLastViewedList(null)
            // Refresh item count only for the list we returned from
            val lastViewedList = lastViewedListId?.let { viewModel.getById(it) }
            if (lastViewedList != null) {
                refreshListCount(lastViewedList)
            }
        }
        // Refresh the title, it may be changed on another device
        // (the cached title is already shown, see titleListener)
        viewLifecycleOwner.lifecycleScope.launch {
            showTitle(UserSettingsRepository.refreshSettings().customTitle)
        }
    }

    /**
     * Show the title, but only while the list overview is shown:
     * after a DB read another screen (with its own title) may be open already.
     */
    private fun showTitle(customTitle: String?) {
        if (findNavController().currentDestination?.id != R.id.AllListsFragment)
            return

        val title = customTitle ?: getString(R.string.all_lists_fragment_default_label)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title
    }

    override fun onClickAdd() {
        // Navigate to ListDetailsFragment for creating a new list
        val action = AllListsFragmentDirections.actionAllListsFragmentToListDetailsFragment(null)
        findNavController().navigate(action)
    }

    fun onClickObject(listInfo: ListInfo) {
        // Save the last viewed list
        UserSettingsRepository.saveLastViewedList(listInfo.id)
        // Use SafeArgs for navigation
        val action = AllListsFragmentDirections.actionAllListsFragmentToItemListFragment(listInfo, listInfo.name)
        findNavController().navigate(action)
    }


    private fun refreshListCount(listInfo: ListInfo) {
        lifecycleScope.launch {
            // Find the index of the list to update
            val curList = viewModel.getById(listInfo.id)
            if (curList != null) {
                val newCount = ItemRepository(listInfo).count()
                if (curList.itemCount != newCount) {
                    // ListInfo is a data class with immutable properties, so use copy() to create updated instance
                    val newList = curList.copy(itemCount = newCount)
                    viewModel.saveObject(newList)
                }
            }
        }
    }

    private fun navigateToLastViewedListIfNeeded() {
        // Navigate to last viewed list if it's the initial load
        if (!isInitialLoad)
            return
        isInitialLoad = false

        viewLifecycleOwner.lifecycleScope.launch {
            // may be opened on another device
            val lastListId = UserSettingsRepository.refreshLastViewedList()

            // Check if this fragment is still the current destination before navigating
            // (the DB read takes some time, the user might have navigated already)
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.AllListsFragment)
                return@launch

            val lastList = lastListId?.let { viewModel.getById(it) }
            if (lastList != null) {
                // Use SafeArgs for navigation
                val action = AllListsFragmentDirections.actionAllListsFragmentToItemListFragment(lastList, lastList.name)
                navController.navigate(action)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
