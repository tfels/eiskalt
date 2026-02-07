package de.felsernet.android.eiskalt

import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
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
     * Load all groups
     */
    fun loadGroups() {
        viewModelScope.launch {
            try {
                _list.value = repository.getAll().sortedBy { it.name.lowercase() }
            } catch (e: FirebaseFirestoreException) {
                sharedMessageViewModel.showErrorMessage("Error loading groups: ${e.message}")
            }
        }
    }

    /**
     * Save the current group
     */
    fun saveGroup(group: Group) {
        if (group.name.isBlank()) {
            sharedMessageViewModel.showErrorMessage("Group name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                if (group.id.isNotEmpty()) {
                    repository.update(group)
                } else {
                    repository.save(group)
                }

                // Update current group with saved data
                loadGroups() // Refresh the list
                _navigateBack.emit(Unit)
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("Error saving group \"${group.name}\": ${e.message}")
            }
        }
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

                loadGroups() // Refresh the list after deletion
            } catch (e: Exception) {
                sharedMessageViewModel.showSuccessMessage("Error deleting group \"${group.name}\": ${e.message}")
            }
        }
    }
}
