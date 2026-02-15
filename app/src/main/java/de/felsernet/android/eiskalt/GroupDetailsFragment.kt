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
    override val newObjectTitle = "New Group"
    override val iconFilePrefix = "group_"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getCurrentObject(): Group {
        // Use SafeArgs to get the group argument (nullable for new groups)
        val args = GroupDetailsFragmentArgs.fromBundle(requireArguments())
        return args.dataObject ?: Group("")
    }

    override fun setupSpecificGuiElements(obj: Group) {
        // Set up UI
    }

    override fun getSpecificChanges(obj: Group) {
        // Update the group
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
