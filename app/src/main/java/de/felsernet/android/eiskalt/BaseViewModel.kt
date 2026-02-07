package de.felsernet.android.eiskalt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

}
