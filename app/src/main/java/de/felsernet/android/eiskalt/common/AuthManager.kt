package de.felsernet.android.eiskalt.common

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.log.AppLog

object AuthManager {
    private const val RC_GOOGLE_SIGN_IN = 9001

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

    private fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    fun signInWithGoogle(activity: Activity) {
        val googleSignInClient = getGoogleSignInClient(activity)
        val accountTask = googleSignInClient.silentSignIn()
        accountTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Already signed in → authenticate with Firebase (no dialog needed)
                // if the cached token is rejected (e.g. stale), fall back to the interactive sign-in
                signInFirebaseWithGoogleAccount(task) {
                    showInteractiveSignIn(activity)
                }
            } else {
                // Not signed in → show explanation dialog first, then Google sign-in UI
                showGoogleSignInExplanationDialog(activity) {
                    activity.startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_SIGN_IN)
                }
            }
        }
    }

    /**
     * Explicit re-login (menu "Sign In / Reconnect"):
     * forget the cached Google account and always show the account picker
     */
    fun reconnect(activity: Activity) {
        showInteractiveSignIn(activity)
    }

    /**
     * Sign out from the cached Google account, then start the Google sign-in UI
     */
    private fun showInteractiveSignIn(activity: Activity) {
        val googleSignInClient = getGoogleSignInClient(activity)
        googleSignInClient.signOut().addOnCompleteListener {
            activity.startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_SIGN_IN)
        }
    }

    private fun showGoogleSignInExplanationDialog(activity: Activity, onProceed: () -> Unit) {
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

    fun handleSignInResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            signInFirebaseWithGoogleAccount(GoogleSignIn.getSignedInAccountFromIntent(data))
            return true
        }
        return false
    }

    /**
     * Sign in to Firebase with the ID token of the Google account from the given (completed) task
     * @param onInvalidCredentials called instead of reporting an error, if Firebase rejects the token
     */
    private fun signInFirebaseWithGoogleAccount(googleTask: Task<GoogleSignInAccount>, onInvalidCredentials: (() -> Unit)? = null) {
        val idToken = try {
            googleTask.getResult(ApiException::class.java).idToken ?: throw IllegalStateException("Google account has no ID token")
        } catch (e: Exception) {
            AppLog.e("auth", "Google sign-in failed", e)
            _authError.value = e.localizedMessage ?: e.message ?: "Authentication failed"
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // User is now signed in with Firebase, the AuthStateListener updates authState
                } else if (authTask.exception is FirebaseAuthInvalidCredentialsException && onInvalidCredentials != null) {
                    AppLog.w("auth", "Cached Google token rejected, asking for new credentials", authTask.exception)
                    onInvalidCredentials()
                } else {
                    AppLog.e("auth", "Firebase sign-in failed", authTask.exception)
                    _authError.value = authTask.exception?.localizedMessage ?: "Sign-in failed"
                }
            }
    }

    sealed class AuthState {
        data class Authenticated(val user: FirebaseUser) : AuthState()
        object Unauthenticated : AuthState()
    }
}
