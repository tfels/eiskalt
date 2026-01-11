package de.felsernet.android.eiskalt

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestoreException

object AppUtils {
    /**
     * Handle Firebase Firestore exceptions with consistent error messages
     */
    fun handleFirestoreException(context: Context, e: FirebaseFirestoreException, operation: String = "load data") {
        when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                Toast.makeText(context, "Cloud access denied. App cannot $operation.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(context, "Failed to $operation", Toast.LENGTH_SHORT).show()
                throw e
            }
        }
    }
}
