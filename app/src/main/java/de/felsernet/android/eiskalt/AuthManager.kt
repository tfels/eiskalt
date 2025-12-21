package de.felsernet.android.eiskalt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object AuthManager {
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

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

    sealed class AuthState {
        data class Authenticated(val user: FirebaseUser) : AuthState()
        object Unauthenticated : AuthState()
    }
}
