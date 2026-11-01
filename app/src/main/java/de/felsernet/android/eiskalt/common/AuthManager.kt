package de.felsernet.android.eiskalt.common

import android.app.AlertDialog
import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.launch

object AuthManager {
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _authError = MutableLiveData<String>()
    val authError: LiveData<String> = _authError

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            _authState.value = AuthState.Authenticated(user)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun initialize() {
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
        // Check current state
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun cleanup() {
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    fun signInWithGoogle(activity: ComponentActivity) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            // Firebase keeps the session (and refreshes its tokens) itself, no Google sign-in needed
            return
        }
        // Not signed in → show explanation dialog first, then the Google account selection
        showGoogleSignInExplanationDialog(activity) {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(activity.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .build()
            signInFirebaseWithGoogleCredential(activity, option)
        }
    }

    /**
     * Explicit re-login (menu "Sign In / Reconnect"):
     * forget the selected Google account and always show the account picker
     */
    fun reconnect(activity: ComponentActivity) {
        activity.lifecycleScope.launch {
            try {
                CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                AppLog.w("auth", "Clearing the credential state failed", e)
            }
            val option = GetSignInWithGoogleOption.Builder(activity.getString(R.string.default_web_client_id)).build()
            signInFirebaseWithGoogleCredential(activity, option)
        }
    }

    private fun showGoogleSignInExplanationDialog(activity: ComponentActivity, onProceed: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.google_signin_explanation_title)
            .setMessage(R.string.google_signin_explanation_message)
            .setPositiveButton(R.string.google_signin_proceed) { dialog, _ ->
                dialog.dismiss()
                onProceed()
            }
            .setNegativeButton(R.string.google_signin_cancel) { dialog, _ ->
                dialog.dismiss()
                // User chose not to sign in - they can still use the app but won't have sync functionality
            }
            .setCancelable(false)
            .create()
            .show()
    }

    /**
     * Let the user select a Google account with the Credential Manager,
     * then sign in to Firebase with its ID token
     */
    private fun signInFirebaseWithGoogleCredential(activity: ComponentActivity, option: CredentialOption) {
        activity.lifecycleScope.launch {
            val idToken = try {
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val credential = CredentialManager.create(activity).getCredential(activity, request).credential
                if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    throw IllegalStateException("Unexpected credential type ${credential.type}")
                }
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } catch (_: GetCredentialCancellationException) {
                // User closed the account selection - they can still use the app but won't have sync functionality
                AppLog.i("auth", "Google sign-in cancelled by user")
                return@launch
            } catch (e: NoCredentialException) {
                AppLog.e("auth", "No Google account available for sign-in", e)
                _authError.value = "No Google account found on this device"
                return@launch
            } catch (e: Exception) {
                AppLog.e("auth", "Google sign-in failed", e)
                _authError.value = e.localizedMessage ?: e.message ?: "Authentication failed"
                return@launch
            }

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        // User is now signed in with Firebase, the AuthStateListener updates authState
                    } else {
                        AppLog.e("auth", "Firebase sign-in failed", authTask.exception)
                        _authError.value = authTask.exception?.localizedMessage ?: "Sign-in failed"
                    }
                }
        }
    }

    sealed class AuthState {
        data class Authenticated(val user: FirebaseUser) : AuthState()
        object Unauthenticated : AuthState()
    }
}
