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

    private fun List<T>.getSortedIndex(obj: T): Int {
        return binarySearch {
            it.name.lowercase().compareTo(obj.name.lowercase())
        }.let { if (it < 0) -it - 1 else it }
    }
    private fun List<T>.insert(index: Int, obj: T): List<T> {
        return toMutableList().apply { add(index, obj) }
    }
    private fun List<T>.replace(index: Int, obj: T): List<T> {
        return toMutableList().apply { set(index, obj) }
    }
    private fun List<T>.delete(obj: T): List<T> {
        return toMutableList().apply { remove(obj) }
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
                    val currentIndex = _list.value.getSortedIndex(obj)
                    _list.value = _list.value.replace(currentIndex, obj)
                } else {
                    repository.save(obj)
                    val insertIndex = _list.value.getSortedIndex(obj)
                    _list.value = _list.value.insert(insertIndex, obj)
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
    fun deleteObject(obj: T) {
        // because of a bad architecture, the object was already deleted
        // in the ViewHolder objectList but not in this ViewModel
        // so we delete it here (watcher might get notified)
        // and reload on error --> watcher updates his ObjectList
        //
        // A race condition may occur when the two list updates are too fast,
        // the system will detect _list has not changed and the watcher is not
        // notified!
        // --> need to rework the architecture so that BaseListFragment knows
        //     about the ViewModel and setupSwipeToDelete can work with it
        viewModelScope.launch {
            try {
                _list.value = _list.value.delete(obj)
                when (val result = _deleteFunc(obj)) {
                    is DeleteResult.Ok -> {
                        sharedMessageViewModel.showSuccessMessage("${typeName} \"${obj.name}\" deleted successfully")
                    }
                    is DeleteResult.Error -> {
                        sharedMessageViewModel.showErrorMessage(result.message)
                        // Reload from DB to sync state - the object was never deleted
                        loadData()
                    }
                }
            } catch (e: Exception) {
                sharedMessageViewModel.showErrorMessage("Error deleting ${typeName} \"${obj.name}\": ${e.message}")
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
