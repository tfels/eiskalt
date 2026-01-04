package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import de.felsernet.android.eiskalt.ListFragmentUtils.setupSwipeToDelete
import de.felsernet.android.eiskalt.databinding.FragmentGroupListBinding
import kotlinx.coroutines.launch

class GroupListFragment : Fragment() {

    private var _binding: FragmentGroupListBinding? = null
    private val binding get() = _binding!!
    private lateinit var groupAdapter: GroupAdapter
    private val groupRepository = GroupRepository.getInstance()
    private var groupsList: MutableList<Group> = mutableListOf()

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
        groupAdapter = GroupAdapter(groupsList) { group ->
            handleGroupClick(group)
        }

        binding.recyclerViewGroups.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupAdapter
        }

        // Load groups
        loadGroups()

        // Set up add group FAB
        binding.fabAddGroup.setOnClickListener {
            findNavController().navigate(R.id.action_GroupListFragment_to_GroupFragment)
        }

        // Set up fragment result listener for group updates
        parentFragmentManager.setFragmentResultListener("groupUpdate", viewLifecycleOwner) { _, bundle ->
            val updatedGroup = bundle.getSerializable("updatedGroup") as? Group
            val isNewGroup = bundle.getBoolean("isNewGroup", false)

            if (updatedGroup != null) {
                if (isNewGroup) {
                    // New group was created, refresh the list
                    loadGroups()
                } else {
                    // Existing group was updated, refresh the list
                    loadGroups()
                }
            }
        }
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                val groups = groupRepository.getAllGroups()
                groupRepository.initializeCounter(groups)
                groupsList.clear()
                groupsList.addAll(groups)
                groupAdapter.notifyDataSetChanged()

                // Set up swipe-to-delete functionality
                // We need to set it up every time groups are loaded to ensure new items are swipeable
                setupSwipeToDelete(
                    recyclerView = binding.recyclerViewGroups,
                    dataList = groupsList,
                    adapter = groupAdapter,
                    deleteMessage = "Group deleted"
                ) { group: Group ->
                    deleteGroup(group)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading groups: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreateGroupDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_group, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextGroupName)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Create New Group")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val groupName = editText.text.toString().trim()
                if (groupName.isNotBlank()) {
                    addNewGroup(groupName)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        editText.addTextChangedListener { text ->
            val isEnabled = text.toString().trim().isNotBlank()
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
        }

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }

        dialog.show()
    }

    private fun addNewGroup(groupName: String) {
        lifecycleScope.launch {
            try {
                val newGroup = Group(groupName)
                groupRepository.saveGroup(newGroup)
                loadGroups()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error adding group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGroupClick(group: Group) {
        // Click on group item triggers edit
        val bundle = Bundle().apply {
            putSerializable("group", group)
        }
        findNavController().navigate(R.id.action_GroupListFragment_to_GroupFragment, bundle)
    }

    private fun showRenameDialog(group: Group) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rename_group, null)
        val editText = dialogView.findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.editTextRenameGroup)
        editText.setText(group.name)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.rename_group)
            .setView(dialogView)
            .setPositiveButton(R.string.rename_group) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameGroup(group, newName)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun renameGroup(group: Group, newName: String) {
        lifecycleScope.launch {
            try {
                group.name = newName
                groupRepository.updateGroup(group)
                loadGroups()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error renaming group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteGroup(group: Group) {
        lifecycleScope.launch {
            try {
                // First delete from database
                groupRepository.deleteGroup(group.id)

                // Then refresh the list to ensure UI consistency with database
                // This handles the case where the group might still be in the local list
                // due to the swipe-to-delete UNDO functionality
                loadGroups()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error deleting group: ${e.message}", Toast.LENGTH_SHORT).show()
                // If deletion fails, reload to ensure UI consistency
                loadGroups()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
