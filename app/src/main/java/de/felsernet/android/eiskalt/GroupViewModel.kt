package de.felsernet.android.eiskalt

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel : BaseViewModel<Group>() {

    private lateinit var groupRepository: GroupRepository
    override val repository get() = groupRepository
    override val typeName: String = "group"

    private val _itemsInGroup = MutableStateFlow<List<Item>>(emptyList())
    val itemsInGroup = _itemsInGroup.asStateFlow()

    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        super.initialize(sharedMessageViewModel)
        groupRepository = GroupRepository.getInstance()
    }

    fun loadItemsForGroup(groupId: String) {
        if (groupId.isEmpty()) {
            _itemsInGroup.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _itemsInGroup.value = groupRepository.getItemsUsingGroup(groupId).sortedBy { it.name.lowercase() }
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("Error loading items for group: ${e.message}")
            }
        }
    }

    /**
     * Delete a group safely (checks if it's used by items)
     */
    override suspend fun _deleteFunc(obj: Group): DeleteResult {
        // Attempt to delete the group - safeDelete is only in GroupRepository
        val result = repository.safeDelete(obj.id)
        val deletionSuccessful = result.first
        val itemsUsingGroup = result.second

        if (!deletionSuccessful) {
            // Group is still being used by items, inform the user via ViewModel
            val message = if (itemsUsingGroup == 1) {
                "Cannot delete group \"${obj.name}\".\n1 item is still using this group."
            } else {
                "Cannot delete group \"${obj.name}\".\n$itemsUsingGroup items are still using this group."
            }
            return DeleteResult.Error(message)
        } else {
            return DeleteResult.Ok
        }
    }
}
