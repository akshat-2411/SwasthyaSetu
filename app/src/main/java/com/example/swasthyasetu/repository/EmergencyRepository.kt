package com.example.swasthyasetu.repository

import com.example.swasthyasetu.model.EmergencyContact
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.jvm.java

class EmergencyRepository {
    private val firestore = FirebaseFirestore.getInstance()
    companion object {
        private const val COLLECTION_EMERGENCY_CONTACTS = "emergency_contacts"
    }
    suspend fun getPrimaryEmergencyContact(): EmergencyContact? {
        return try {
            val snapshot = firestore
                .collection(COLLECTION_EMERGENCY_CONTACTS)
                .limit(1)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(EmergencyContact::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            throw e
        }
    }
    suspend fun getAllEmergencyContacts(): List<EmergencyContact> {
        return try {
            val snapshot = firestore
                .collection(COLLECTION_EMERGENCY_CONTACTS)
                .get()
                .await()
            snapshot.toObjects(EmergencyContact::class.java)
        } catch (e: Exception) {
            throw e
        }
    }
    suspend fun saveEmergencyContact(contact: EmergencyContact): String {
        return try {
            val docRef = firestore
                .collection(COLLECTION_EMERGENCY_CONTACTS)
                .add(contact)
                .await()
            docRef.id
        } catch (e: Exception) {
            throw e
        }
    }
}