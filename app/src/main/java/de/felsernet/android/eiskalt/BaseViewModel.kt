package de.felsernet.android.eiskalt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class providing common state management for entities.
 * Handles StateFlow for list data and SharedFlow for navigation events.
 *
 * @param T The data type this ViewModel manages, must implement BaseDataClass
 */
abstract class BaseViewModel<T : BaseDataClass> : ViewModel() {

    // StateFlow for the list of entities (reactive UI updates)
    protected val _list = MutableStateFlow<List<T>>(emptyList())
    val list = _list.asStateFlow()

    // SharedFlow for one-time navigation event
    protected val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack = _navigateBack.asSharedFlow()

    protected lateinit var sharedMessageViewModel: SharedMessageViewModel
    protected abstract val repository: BaseRepository<T>
    protected abstract val typeName: String

    /**
     * Initialize the ViewModel with required dependencies
     */
    open fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        this.sharedMessageViewModel = sharedMessageViewModel
    }

    /**
     * Load list of objects from DB
     */
    fun loadData() {
        viewModelScope.launch {
            try {
                _list.value = repository.getAll().sortedBy { it.name.lowercase() }
            } catch (e: FirebaseFirestoreException) {
                sharedMessageViewModel.showErrorMessage("Error loading ${typeName}s: ${e.message}")
            }
        }
    }

    /**
     * Save the given object to DB
     */
    fun saveObject(obj: T) {
        if (obj.name.isBlank()) {
            sharedMessageViewModel.showErrorMessage("${typeName} name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                if (obj.id.isNotEmpty()) {
                    repository.update(obj)
                } else {
                    repository.save(obj)
                }

                // Update our list with saved data
                loadData()
                _navigateBack.emit(Unit)
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("Error saving ${typeName} \"${obj.name}\": ${e.message}")
            }
        }
    }

    /**
     * Delete an object from the DB
     * Override _deleteFunc if you need to change behaviour
     */
    fun deleteObject(obj: T) {
        viewModelScope.launch {
            try {
                _deleteFunc(obj)
                loadData() // Refresh the list after deletion
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("Error deleting ${typeName} \"${obj.name}\": ${e.message}")
            }
        }
    }

    @Suppress("FunctionName")
    protected open suspend fun _deleteFunc(obj: T) {
        repository.delete(obj.id)
    }

}
