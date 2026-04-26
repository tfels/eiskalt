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

    // SharedFlow for one-time events
    protected val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack = _navigateBack.asSharedFlow()
    protected val _dataLoaded = MutableSharedFlow<Unit>()
    val dataLoaded = _dataLoaded.asSharedFlow()

    protected lateinit var sharedMessageViewModel: SharedMessageViewModel
    protected abstract val repository: BaseRepository<T>
    protected abstract val typeName: String

    private fun List<T>.getSortedIndex(obj: T): Int {
        return binarySearch {
            it.name.lowercase().compareTo(obj.name.lowercase())
        }.let { if (it < 0) -it - 1 else it }
    }
    private fun List<T>.insert(obj: T): List<T> {
        val insertIndex = getSortedIndex(obj)
        return toMutableList().apply { add(insertIndex, obj) }
    }
    private fun List<T>.replace(obj: T): List<T> {
        // Item might have been renamed, so remove old version and re-insert at new sorted position
        return delete(obj).insert(obj)
    }
    private fun List<T>.delete(obj: T): List<T> {
        return filter { it.id != obj.id }
    }

    /**
     * Remove an object from the list immediately for UI feedback (e.g. swipe to delete)
     */
    fun onSwipeToDelete(obj: T) {
        _list.value = _list.value.delete(obj)
    }

    /**
     * Restore an object to the list (e.g. after UNDO)
     */
    fun onUndoDelete(obj: T) {
        if (!_list.value.any { it.id == obj.id }) {
            _list.value = _list.value.insert(obj)
        }
    }

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
                _dataLoaded.emit(Unit)
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
                    _list.value = _list.value.replace(obj)
                } else {
                    repository.save(obj)
                    _list.value = _list.value.insert(obj)
                }

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
    fun deleteObjectFromDb(obj: T) {
        viewModelScope.launch {
            try {
                when (val result = _deleteFunc(obj)) {
                    is DeleteResult.Ok -> {
                        sharedMessageViewModel.showSuccessMessage("${typeName} \"${obj.name}\" deleted successfully")
                    }
                    is DeleteResult.Error -> {
                        sharedMessageViewModel.showErrorMessage(result.message)
                        onUndoDelete(obj)
                    }
                }
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("Error deleting ${typeName} \"${obj.name}\": ${e.message}")
                onUndoDelete(obj)
            }
        }
    }

    sealed class DeleteResult {
        object Ok : DeleteResult()
        data class Error(val message: String) : DeleteResult()
    }

    @Suppress("FunctionName")
    protected open suspend fun _deleteFunc(obj: T): DeleteResult {
        repository.delete(obj.id)
        return DeleteResult.Ok
    }

}
