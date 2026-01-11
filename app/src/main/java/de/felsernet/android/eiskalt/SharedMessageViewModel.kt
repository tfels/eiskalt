package de.felsernet.android.eiskalt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for handling messages that should persist across fragment navigation.
 * This allows error messages to be shown even when the user has navigated away from
 * the original fragment where the error occurred.
 *
 * Uses SharedFlow instead of StateFlow because:
 * - Messages are one-time events, not persistent state
 * - No need to replay old messages to new collectors
 * - Better semantics for event-based communication
 * - No artificial initial state required
 */
class SharedMessageViewModel : ViewModel() {

    // SharedFlow for error messages (no replay, as messages are one-time events)
    private val _errorMessage = MutableSharedFlow<String>(replay = 0)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    // SharedFlow for success messages (no replay, as messages are one-time events)
    private val _successMessage = MutableSharedFlow<String>(replay = 0)
    val successMessage: SharedFlow<String> = _successMessage.asSharedFlow()

    /**
     * Show an error message
     */
    fun showErrorMessage(message: String) {
        viewModelScope.launch {
            _errorMessage.emit(message)
        }
    }

    /**
     * Show a success message
     */
    fun showSuccessMessage(message: String) {
        viewModelScope.launch {
            _successMessage.emit(message)
        }
    }
}
