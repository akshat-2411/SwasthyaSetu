package com.example.swasthyasetu.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun firebaseAuthWithGoogle(credential: AuthCredential): AuthResult {
        return firebaseAuth.signInWithCredential(credential).await()
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
