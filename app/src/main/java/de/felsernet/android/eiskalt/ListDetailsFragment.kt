package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import de.felsernet.android.eiskalt.databinding.FragmentListDetailsBinding

/**
 * Fragment for managing lists (add/edit) similar to GroupDetailsFragment
 * Uses ViewModel with Flows for state management and data sharing.
 */
class ListDetailsFragment : BaseDetailsFragment<ListInfo>() {

    private var _binding: FragmentListDetailsBinding? = null
    private val binding get() = _binding!!
    // Shared ViewModel survives fragment recreation
    override val viewModel: ListViewModel by activityViewModels()
    override val newObjectTitle = "New List"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getCurrentObject(): ListInfo {
        // Use SafeArgs to get the list argument (nullable for new lists)
        val args = ListDetailsFragmentArgs.fromBundle(requireArguments())
        return args.dataObject ?: ListInfo("", "", 0)
    }

    override fun setupSpecificGuiElements(obj: ListInfo) {
    }

    override fun getSpecificChanges(obj: ListInfo) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}