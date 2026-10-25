package de.felsernet.android.eiskalt.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import de.felsernet.android.eiskalt.common.SharedMessageViewModel
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

    // StateFlow for the display list (includes grouping/headers if needed)
    protected val _displayList = MutableStateFlow<List<DisplayItem<T>>>(emptyList())
    val displayList = _displayList.asStateFlow()

    // SharedFlow for one-time events
    protected val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack = _navigateBack.asSharedFlow()
    protected val _dataLoaded = MutableSharedFlow<Unit>()
    val dataLoaded = _dataLoaded.asSharedFlow()

    // true while loadData() is running
    protected val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

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
     * Initialize the ViewModel with required dependencies
     */
    open fun initialize(sharedMessageViewModel: SharedMessageViewModel) {
        this.sharedMessageViewModel = sharedMessageViewModel
    }

    /**
     * Rebuild the display list from the raw list.
     * Default implementation just wraps each item in a DisplayItem.Content.
     * Subclasses can override this to implement grouping.
     */
    protected open fun rebuildDisplayList() {
        _displayList.value = _list.value.map { DisplayItem.Content(it) }
    }

    /**
     * Load list of objects from DB
     */
    open fun loadData() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _list.value = repository.getAll().sortedBy { it.name.lowercase() }
                rebuildDisplayList()
                _dataLoaded.emit(Unit)
            } catch (e: FirebaseFirestoreException) {
                sharedMessageViewModel.showErrorMessage("db", "Error loading ${typeName}s: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Forget the loaded data, e.g. when a shared ViewModel is used for another list next time,
     * so the old content is not shown until the new one is loaded
     */
    fun clearData() {
        _list.value = emptyList()
        _displayList.value = emptyList()
    }

    /**
     * Get an object from the list by id
     */
    fun getById(id: String) : T? {
        return _list.value.find { it.id == id }
    }

    /**
     * Remove an object from the list immediately for UI feedback (e.g. swipe to delete)
     */
    fun deleteFromList(obj: T) {
        _list.value = _list.value.delete(obj)
        rebuildDisplayList()
    }

    /**
     * Restore an object to the list (e.g. after UNDO)
     */
    fun undoDeleteFromList(obj: T) : Boolean {
        if (!_list.value.any { it.id == obj.id }) {
            _list.value = _list.value.insert(obj)
            rebuildDisplayList()
            return true
        }
        return false
    }

    /**
     * Save the given object to DB
     */
    fun saveObject(obj: T) {
        if (obj.name.isBlank()) {
            sharedMessageViewModel.showErrorMessage("input", "${typeName} name cannot be empty")
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
                rebuildDisplayList()

                _navigateBack.emit(Unit)
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("db", "Error saving ${typeName} \"${obj.name}\": ${e.message}", e)
            }
        }
    }

    /**
     * Delete an object from the DB
     * Override _deleteFunc if you need to change behaviour
     */
    fun deleteFromDb(obj: T) {
        viewModelScope.launch {
            try {
                when (val result = _deleteFunc(obj)) {
                    is DeleteResult.Ok -> {
                        sharedMessageViewModel.showSuccessMessage("db", "${typeName} \"${obj.name}\" deleted successfully")
                    }
                    is DeleteResult.Error -> {
                        sharedMessageViewModel.showErrorMessage("db", result.message)
                        undoDeleteFromList(obj)
                    }
                }
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("db", "Error deleting ${typeName} \"${obj.name}\": ${e.message}", e)
                undoDeleteFromList(obj)
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
