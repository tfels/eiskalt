package de.felsernet.android.eiskalt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData

/**
 * Shared ViewModel for handling messages that should persist across fragment navigation.
 * This allows error messages to be shown even when the user has navigated away from
 * the original fragment where the error occurred.
 */
class SharedMessageViewModel : ViewModel() {

    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // LiveData for success messages
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    /**
     * Show an error message
     */
    fun showErrorMessage(message: String) {
        _errorMessage.value = message
    }

    /**
     * Show a success message
     */
    fun showSuccessMessage(message: String) {
        _successMessage.value = message
    }

    /**
     * Clear the current error message (after it has been shown)
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Clear the current success message (after it has been shown)
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
