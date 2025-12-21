package de.felsernet.android.eiskalt

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

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

    fun signInWithGoogle(activity: Activity) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        val accountTask = googleSignInClient.silentSignIn()
        accountTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Already signed in → authenticate with Firebase
                firebaseAuthWithGoogle(task.result?.idToken!!)
            } else {
                // Not signed in → show Google sign-in UI
                activity.startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_SIGN_IN)
            }
        }
    }

    fun handleSignInResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(Exception::class.java)
                firebaseAuthWithGoogle(account?.idToken!!)
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: e.message ?: "Authentication failed"
            }
            return true
        }
        return false
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    // User is now signed in with Firebase
                } else {
                    _authError.value = task.exception?.localizedMessage ?: "Sign-in failed"
                }
            }
    }

    sealed class AuthState {
        data class Authenticated(val user: FirebaseUser) : AuthState()
        object Unauthenticated : AuthState()
    }
}
