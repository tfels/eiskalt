package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import de.felsernet.android.eiskalt.databinding.FragmentGroupListBinding
import kotlinx.coroutines.launch

class GroupListFragment : BaseListFragment<Group>() {

    private var _binding: FragmentGroupListBinding? = null
    private val binding get() = _binding!!
    override val recyclerView: RecyclerView get() = binding.recyclerViewGroups
    override val fabView: View get() = binding.fabAddGroup
    override val deleteMessage: String = "Group deleted"
    override val adapterLayoutId: Int = R.layout.item_group
    override val adapterViewHolderFactory: (View) -> GroupViewHolder get() = ::GroupViewHolder
    private val groupRepository = GroupRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up fragment result listener for group updates
        parentFragmentManager.setFragmentResultListener("groupUpdate", viewLifecycleOwner) { _, bundle ->
            val updatedGroup = bundle.getSerializable("updatedGroup") as? Group
            val isNewGroup = bundle.getBoolean("isNewGroup", false)

            if (updatedGroup != null) {
                loadData()
            }
        }
    }

    override fun loadData() {
        lifecycleScope.launch {
            try {
                val groups = groupRepository.getAll()
                objectsList.clear()
                objectsList.addAll(groups.sortedBy { it.name.lowercase() })
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("Error loading groups: ${e.message}")
            }
        }
    }

    override fun onClickAdd() {
        findNavController().navigate(R.id.action_GroupListFragment_to_GroupFragment)
    }

    override suspend fun onSwipeDelete(group: Group) {
        deleteGroup(group)
    }

    override fun onClickObject(group: Group) {
        // Click on group item triggers edit
        // Use SafeArgs for navigation
        val action = GroupListFragmentDirections.actionGroupListFragmentToGroupFragment(group)
        findNavController().navigate(action)
    }

    private suspend fun deleteGroup(group: Group) {
        try {
            // Attempt to delete the group
            val (deletionSuccessful, itemsUsingGroup) = groupRepository.safeDelete(group.id)

            if (deletionSuccessful) {
                // Group was deleted successfully, refresh the list
                loadData()
            } else {
                // Group is still being used by items, inform the user via ViewModel
                val message = if (itemsUsingGroup == 1) {
                    "Cannot delete group. 1 item is still using this group."
                } else {
                    "Cannot delete group. $itemsUsingGroup items are still using this group."
                }
                sharedMessageViewModel.showErrorMessage(message)

                // Reload groups to ensure UI consistency
                loadData()
            }
        } catch (e: Exception) {
            // Show error via ViewModel instead of direct Toast
            sharedMessageViewModel.showErrorMessage("Error deleting group: ${e.message}")
            // If deletion fails, reload to ensure UI consistency
            loadData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
