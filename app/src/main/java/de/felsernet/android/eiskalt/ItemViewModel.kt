package de.felsernet.android.eiskalt

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ItemViewModel : BaseViewModel<Item>() {

    private lateinit var itemRepository: ItemRepository
    override val repository get() = itemRepository
    override val typeName: String = "item"

    @Deprecated("Use overloaded initialize instead")
    override fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        throw UnsupportedOperationException("Use overloaded initialize instead")
    }

    fun initialize(sharedMessageViewModel: SharedMessageViewModel, listName: String) {
        super.initialize(sharedMessageViewModel)
        this.itemRepository = ItemRepository(listName)
    }

    /**
     * Delete an item
     */
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.delete(item.id)
                loadData() // Refresh the list after deletion
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("Error deleting item \"${item.name}\": ${e.message}")
            }
        }
    }
}
