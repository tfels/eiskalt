package de.felsernet.android.eiskalt

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GroupViewModel : BaseViewModel<Group>() {

    private lateinit var groupRepository: GroupRepository
    override val repository get() = groupRepository
    override val typeName: String = "group"

    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        super.initialize(sharedMessageViewModel)
        groupRepository = GroupRepository.getInstance()
    }

    /**
     * Delete a group safely (checks if it's used by items)
     */
    fun deleteGroup(group: Group) {
        viewModelScope.launch {
            try {
                // Attempt to delete the group - safeDelete is only in GroupRepository
                val result = (repository as GroupRepository).safeDelete(group.id)
                val deletionSuccessful = result.first
                val itemsUsingGroup = result.second

                if (!deletionSuccessful) {
                    // Group is still being used by items, inform the user via ViewModel
                    val message = if (itemsUsingGroup == 1) {
                        "Cannot delete group \"${group.name}\". 1 item is still using this group."
                    } else {
                        "Cannot delete group \"${group.name}\". $itemsUsingGroup items are still using this group."
                    }
                    sharedMessageViewModel.showErrorMessage(message)
                } else {
                    sharedMessageViewModel.showSuccessMessage("Group \"${group.name}\" deleted")
                }

                loadData() // Refresh the list after deletion
            } catch (e: Exception) {
                sharedMessageViewModel.showSuccessMessage("Error deleting group \"${group.name}\": ${e.message}")
            }
        }
    }
}
