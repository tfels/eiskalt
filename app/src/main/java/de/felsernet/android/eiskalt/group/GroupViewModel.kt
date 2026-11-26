package de.felsernet.android.eiskalt.group

import android.app.Application
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseViewModel
import de.felsernet.android.eiskalt.common.SharedMessageViewModel

class GroupViewModel(application: Application) : BaseViewModel<Group>(application) {

    private lateinit var groupRepository: GroupRepository
    override val repository get() = groupRepository
    override val messages = Messages(
        R.string.group_load_error,
        R.string.group_name_empty,
        R.string.group_save_error,
        R.string.group_delete_done,
        R.string.group_delete_error,
    )

    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        super.initialize(sharedMessageViewModel)
        groupRepository = GroupRepository.getInstance()
    }

    /**
     * Get all items (of all lists) using the given group
     */
    suspend fun getItemsUsingGroup(groupId: String): List<GroupRepository.ItemUsage> {
        return GroupRepository.getInstance().getItemsUsingGroup(groupId)
    }

    /**
     * Delete a group safely (checks if it's used by items)
     */
    override suspend fun _deleteFunc(obj: Group): DeleteResult {
        // Attempt to delete the group - safeDelete is only in GroupRepository
        val result = repository.safeDelete(obj.id, obj.name)
        val deletionSuccessful = result.first
        val itemsUsingGroup = result.second

        if (!deletionSuccessful) {
            // Group is still being used by items, inform the user via ViewModel
            val message = getApplication<Application>().resources
                .getQuantityString(R.plurals.group_delete_in_use, itemsUsingGroup, obj.name, itemsUsingGroup)
            return DeleteResult.Error(message)
        } else {
            return DeleteResult.Ok
        }
    }
}
