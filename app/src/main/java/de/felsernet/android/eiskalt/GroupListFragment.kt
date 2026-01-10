package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.felsernet.android.eiskalt.databinding.FragmentGroupListBinding
import kotlinx.coroutines.launch

class GroupListFragment : BaseListFragment<Group>() {

    private var _binding: FragmentGroupListBinding? = null
    private val binding get() = _binding!!
    override val recyclerView: RecyclerView get() = binding.recyclerViewGroups
    override val fabView: View get() = binding.fabAddGroup
    override val deleteMessage: String = "Group deleted"

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

        // Set up RecyclerView
        adapter = GroupListAdapter(objectsList) { group ->
            handleGroupClick(group)
        }

        binding.recyclerViewGroups.layoutManager = LinearLayoutManager(requireContext())

        // Set up add group FAB and swipe-to-delete functionality
        setupListFunctionality()

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
                objectsList.addAll(groups)
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading groups: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onClickAdd() {
        findNavController().navigate(R.id.action_GroupListFragment_to_GroupFragment)
    }

    override suspend fun onSwipeDelete(group: Group) {
        deleteGroup(group)
    }

    private fun handleGroupClick(group: Group) {
        // Click on group item triggers edit
        val bundle = Bundle().apply {
            putSerializable("group", group)
        }
        findNavController().navigate(R.id.action_GroupListFragment_to_GroupFragment, bundle)
    }

    private suspend fun deleteGroup(group: Group) {
        try {
            // Attempt to delete the group
            val (deletionSuccessful, itemsUsingGroup) = groupRepository.safeDelete(group.id)

            if (deletionSuccessful) {
                // Group was deleted successfully, refresh the list
                loadData()
            } else {
                // Group is still being used by items, inform the user
                val message = if (itemsUsingGroup == 1) {
                    "Cannot delete group. 1 item is still using this group."
                } else {
                    "Cannot delete group. $itemsUsingGroup items are still using this group."
                }
                activity?.runOnUiThread {
                    activity?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
                }

                // Reload groups to ensure UI consistency
                loadData()
            }
        } catch (e: Exception) {
            activity?.runOnUiThread {
                activity?.let { Toast.makeText(it, "Error deleting group: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            // If deletion fails, reload to ensure UI consistency
            loadData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
