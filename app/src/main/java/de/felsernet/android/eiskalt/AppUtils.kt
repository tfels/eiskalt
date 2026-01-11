package de.felsernet.android.eiskalt

import com.google.firebase.firestore.FirebaseFirestoreException

object AppUtils {
    /**
     * Handle Firebase Firestore exceptions with consistent error messages
     */
    fun handleFirestoreException(sharedMessageViewModel: SharedMessageViewModel, e: FirebaseFirestoreException, operation: String = "load data") {
        when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                sharedMessageViewModel.showErrorMessage("Cloud access denied. App cannot $operation.")
            }
            else -> {
                sharedMessageViewModel.showErrorMessage("Failed to $operation")
                throw e
            }
        }
    }
}
