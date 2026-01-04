package de.felsernet.android.eiskalt

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.felsernet.android.eiskalt.databinding.FragmentGroupBinding
import kotlinx.coroutines.launch

/**
 * Fragment for managing groups (add/edit) similar to InventoryItemFragment
 */
class GroupFragment : Fragment() {

    private var _binding: FragmentGroupBinding? = null
    private val binding get() = _binding!!

    private lateinit var currentGroup: Group
    private val groupRepository = GroupRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the group from arguments (null for new group)
        val group = arguments?.getSerializable("group") as? Group
        currentGroup = group ?: Group("")

        // Set the title
        val title = if (currentGroup.name.isNotEmpty()) currentGroup.name else "New Group"
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title

        // Set up UI
        binding.editTextName.setText(currentGroup.name)
        binding.textViewId.text = if (currentGroup.id > 0) currentGroup.id.toString() else "New"
        binding.editTextComment.setText(currentGroup.comment)

        binding.buttonSave.setOnClickListener {
            saveGroupChanges()
            // Navigation is now handled inside the coroutine after successful save
        }

        // Handle back button press to save changes
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            saveGroupChanges()
            findNavController().navigateUp()
        }
    }

    private fun saveGroupChanges(): Boolean {
        val updatedName = binding.editTextName.text.toString().trim()

        if (updatedName.isEmpty()) {
            Toast.makeText(requireContext(), "Group name cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        // Update the group
        currentGroup.name = updatedName
        currentGroup.comment = binding.editTextComment.text.toString().trim()

        lifecycleScope.launch {
            try {
                if (currentGroup.id > 0) {
                    // Existing group - update
                    groupRepository.updateGroup(currentGroup)
                } else {
                    // New group - create
                    groupRepository.saveGroup(currentGroup)
                }

                // Pass the result back to the previous fragment
                val result = Bundle().apply {
                    putSerializable("updatedGroup", currentGroup)
                    putBoolean("isNewGroup", currentGroup.id == 0L)
                }
                parentFragmentManager.setFragmentResult("groupUpdate", result)

                // Navigate back after successful save
                findNavController().navigateUp()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error saving group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        return false // Don't navigate immediately, let coroutine handle it
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
