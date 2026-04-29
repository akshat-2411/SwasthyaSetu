package com.example.swasthyasetu.repository

import com.example.swasthyasetu.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.jvm.java

class ProfileRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val COLLECTION_USERS = "users"
    }

    private fun currentUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User is not authenticated")
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = currentUid()
        firestore
            .collection(COLLECTION_USERS)
            .document(uid)
            .set(profile)
            .await()
    }

    suspend fun fetchUserProfile(): UserProfile? {
        val uid = currentUid()
        val snapshot = firestore
            .collection(COLLECTION_USERS)
            .document(uid)
            .get()
            .await()

        return if (snapshot.exists()) {
            snapshot.toObject(UserProfile::class.java)
        } else {
            null
        }
    }
}
