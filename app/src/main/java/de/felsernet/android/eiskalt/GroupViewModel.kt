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
    override suspend fun _deleteFunc(obj: Group) {
        // Attempt to delete the group - safeDelete is only in GroupRepository
        val result = repository.safeDelete(obj.id)
        val deletionSuccessful = result.first
        val itemsUsingGroup = result.second

        if (!deletionSuccessful) {
            // Group is still being used by items, inform the user via ViewModel
            val message = if (itemsUsingGroup == 1) {
                "Cannot delete group \"${obj.name}\". 1 item is still using this group."
            } else {
                "Cannot delete group \"${obj.name}\". $itemsUsingGroup items are still using this group."
            }
            sharedMessageViewModel.showErrorMessage(message)
        } else {
            sharedMessageViewModel.showSuccessMessage("Group \"${obj.name}\" deleted")
        }
    }
}
