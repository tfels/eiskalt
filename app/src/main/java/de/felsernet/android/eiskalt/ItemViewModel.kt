package de.felsernet.android.eiskalt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ItemViewModel : ViewModel() {

    // StateFlow for the list of items (reactive UI updates)
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items = _items.asStateFlow()

    // SharedFlow for one-time navigation event
    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack = _navigateBack.asSharedFlow()

    private lateinit var itemRepository: ItemRepository
    private lateinit var sharedMessageViewModel: SharedMessageViewModel

    fun initialize(sharedMessageViewModel: SharedMessageViewModel, listName: String) {
        this.sharedMessageViewModel = sharedMessageViewModel
        itemRepository = ItemRepository(listName)
    }

    /**
     * Load all items for a specific list
     */
    fun loadItems() {
        viewModelScope.launch {
            try {
                _items.value = itemRepository.getAll().sortedBy { it.name.lowercase() }
            } catch (e: FirebaseFirestoreException) {
                sharedMessageViewModel.showErrorMessage("Error loading items: ${e.message}")
            }
        }
    }

    /**
     * Save the current item
     */
    fun saveItem(item: Item) {
        if (item.name.isBlank()) {
            sharedMessageViewModel.showErrorMessage("Item name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                if (item.id.isNotEmpty()) {
                    itemRepository.update(item)
                } else {
                    itemRepository.save(item)
                }

                // Update current item with saved data
                loadItems() // Refresh the list
                _navigateBack.emit(Unit)
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("Error saving item \"${item.name}\": ${e.message}")
            }
        }
    }

    /**
     * Delete an item
     */
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                itemRepository.delete(item.id)
                loadItems() // Refresh the list after deletion
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("Error deleting item \"${item.name}\": ${e.message}")
            }
        }
    }
}
