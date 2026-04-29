package com.example.swasthyasetu.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swasthyasetu.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    init { _authState.value = AuthState.Idle }

    fun signInWithGoogleCredential(credential: AuthCredential) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = repository.firebaseAuthWithGoogle(credential)
                val user = result.user
                if (user != null) {
                    Log.d(TAG, "Firebase auth success: ${user.displayName}")
                    _authState.value = AuthState.Success(user)
                } else {
                    _authState.value = AuthState.Error("Sign-in failed. Please try again.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase auth with Google failed", e)
                _authState.value = AuthState.Error(
                    e.localizedMessage ?: "Authentication failed. Please try again."
                )
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _authState.value = AuthState.Idle
    }

    fun resetState() { _authState.value = AuthState.Idle }

    companion object { private const val TAG = "AuthViewModel" }
}
