package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import de.felsernet.android.eiskalt.databinding.FragmentGroupDetailsBinding

/**
 * Fragment for managing groups (add/edit) similar to ItemDetailsFragment
 * Uses ViewModel with Flows for state management and data sharing.
 */
class GroupDetailsFragment : BaseDetailsFragment<Group>() {

    private var _binding: FragmentGroupDetailsBinding? = null
    private val binding get() = _binding!!
    // Shared ViewModel survives fragment recreation
    override val viewModel: GroupViewModel by activityViewModels()

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
        currentObject = args.dataObject ?: Group("")

        // Set the title
        val title = if (currentObject.name.isNotEmpty()) currentObject.name else "New Group"
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title

        // Set up UI
        binding.editTextName.setText(currentObject.name)
        binding.textViewId.text = if (currentObject.id.isNotEmpty()) currentObject.id else "New"
        binding.editTextComment.setText(currentObject.comment)
    }

    override fun saveChanges() {
        // Update the group
        currentObject.name = binding.editTextName.text.toString().trim()
        currentObject.comment = binding.editTextComment.text.toString().trim()

        // Save via ViewModel (validation handled in ViewModel)
        viewModel.saveObject(currentObject)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
