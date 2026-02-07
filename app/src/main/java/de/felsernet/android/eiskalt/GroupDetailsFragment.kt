package de.felsernet.android.eiskalt

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import de.felsernet.android.eiskalt.databinding.FragmentGroupDetailsBinding
import kotlinx.coroutines.launch

/**
 * Fragment for managing groups (add/edit) similar to ItemDetailsFragment
 * Uses ViewModel with Flows for state management and data sharing.
 */
class GroupDetailsFragment : Fragment() {

    private var _binding: FragmentGroupDetailsBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModels (both survive fragment recreation)
    private val viewModel: GroupViewModel by activityViewModels()
    private val sharedMessageViewModel: SharedMessageViewModel by activityViewModels()

    private lateinit var currentGroup: Group

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use SafeArgs to get the group argument (nullable for new groups)
        val args = GroupDetailsFragmentArgs.fromBundle(requireArguments())
        currentGroup = args.group ?: Group("")

        // Set the title
        val title = if (currentGroup.name.isNotEmpty()) currentGroup.name else "New Group"
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title

        // Set up UI
        binding.editTextName.setText(currentGroup.name)
        binding.textViewId.text = if (currentGroup.id.isNotEmpty()) currentGroup.id else "New"
        binding.editTextComment.setText(currentGroup.comment)

        // Collect all flows in a single repeatOnLifecycle block
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.navigateBack.collect {
                        findNavController().navigateUp()
                    }
                }

                launch {
                    sharedMessageViewModel.errorMessage.collect { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.buttonSave.setOnClickListener {
            saveGroupChanges()
            // Navigation is handled after successful save
        }

        // Handle back button press to save changes
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            saveGroupChanges()
            findNavController().navigateUp()
        }
    }

    private fun saveGroupChanges() {
        // Update the group
        currentGroup.name = binding.editTextName.text.toString().trim()
        currentGroup.comment = binding.editTextComment.text.toString().trim()

        // Save via ViewModel (validation handled in ViewModel)
        viewModel.saveGroup(currentGroup)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
