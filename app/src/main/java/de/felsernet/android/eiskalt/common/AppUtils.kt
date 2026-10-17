package de.felsernet.android.eiskalt.common

import com.google.firebase.firestore.FirebaseFirestoreException

object AppUtils {
    /**
     * Handle Firebase Firestore exceptions with consistent error messages
     */
    fun handleFirestoreException(sharedMessageViewModel: SharedMessageViewModel, e: FirebaseFirestoreException, operation: String = "load data") {
        when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                sharedMessageViewModel.showErrorMessage("firestore", "Cloud access denied. App cannot $operation.", e)
            }
            else -> {
                sharedMessageViewModel.showErrorMessage("firestore", "Failed to $operation", e)
                throw e
            }
        }
    }
}
