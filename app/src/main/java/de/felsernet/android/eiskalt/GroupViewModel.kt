package de.felsernet.android.eiskalt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {

    // StateFlow for the list of groups (reactive UI updates)
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups = _groups.asStateFlow()

    // SharedFlow for one-time navigation event
    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack = _navigateBack.asSharedFlow()

    private lateinit var groupRepository: GroupRepository
    private lateinit var sharedMessageViewModel: SharedMessageViewModel

    fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        this.sharedMessageViewModel = sharedMessageViewModel
        groupRepository = GroupRepository.getInstance()
        loadGroups()
    }

    /**
     * Load all groups
     */
    fun loadGroups() {
        viewModelScope.launch {
            try {
                _groups.value = groupRepository.getAll().sortedBy { it.name.lowercase() }
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
                    groupRepository.update(group)
                } else {
                    groupRepository.save(group)
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
                // Attempt to delete the group
                val (deletionSuccessful, itemsUsingGroup) = groupRepository.safeDelete(group.id)

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
