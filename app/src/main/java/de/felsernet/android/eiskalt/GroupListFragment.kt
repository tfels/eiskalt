package de.felsernet.android.eiskalt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

    // Shared ViewModel for groups (survives fragment recreation)
    private val viewModel: GroupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel and load groups
        viewModel.initialize()

        // Collect all flows in a single repeatOnLifecycle block
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.groups.collect {
                        objectsList.clear()
                        objectsList.addAll(it)
                        adapter.notifyDataSetChanged()
                    }
                }

                launch {
                    viewModel.errorMessage.collect { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun loadData() {
        // Data is loaded via ViewModel's Flow - no need for manual load
        viewModel.loadGroups()
    }

    override fun onClickAdd() {
        // Pass null group for new group creation
        val action = GroupListFragmentDirections.actionGroupListFragmentToGroupFragment(null)
        findNavController().navigate(action)
    }

    override suspend fun onSwipeDelete(group: Group) {
        viewModel.deleteGroup(group)
    }

    override fun onClickObject(group: Group) {
        // Click on group item triggers edit
        // Use SafeArgs for navigation
        val action = GroupListFragmentDirections.actionGroupListFragmentToGroupFragment(group)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
