package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import de.felsernet.android.eiskalt.databinding.FragmentGroupListBinding
import kotlinx.coroutines.launch

class GroupListFragment : Fragment() {

    private var _binding: FragmentGroupListBinding? = null
    private val binding get() = _binding!!
    private lateinit var groupAdapter: GroupAdapter
    private val groupRepository = GroupRepository()

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
        groupAdapter = GroupAdapter(mutableListOf()) { group, action ->
            handleGroupAction(group, action)
        }

        binding.recyclerViewGroups.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupAdapter
        }

        // Load groups
        loadGroups()

        // Set up add group button
        binding.buttonAddGroup.setOnClickListener {
            addNewGroup()
        }
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                val groups = groupRepository.getAllGroups()
                groupAdapter.updateGroups(groups.toMutableList())
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading groups: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addNewGroup() {
        val groupName = binding.editTextNewGroupName.text.toString().trim()
        if (groupName.isEmpty()) {
            Toast.makeText(requireContext(), "Group name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val newGroup = Group(groupName)
        lifecycleScope.launch {
            try {
                groupRepository.saveGroup(newGroup)
                binding.editTextNewGroupName.text.clear()
                loadGroups()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error adding group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGroupAction(group: Group, action: String) {
        when (action) {
            "rename" -> showRenameDialog(group)
            "delete" -> deleteGroup(group)
        }
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
                groupRepository.renameGroup(group.id, newName)
                loadGroups()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error renaming group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteGroup(group: Group) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_group)
            .setMessage("Are you sure you want to delete '${group.name}'?")
            .setPositiveButton(R.string.delete_group) { _, _ ->
                lifecycleScope.launch {
                    try {
                        groupRepository.deleteGroup(group.id)
                        loadGroups()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error deleting group: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
