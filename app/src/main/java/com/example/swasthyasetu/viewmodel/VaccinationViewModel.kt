package com.example.swasthyasetu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.swasthyasetu.model.Vaccine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.jvm.java

class VaccinationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserUid = auth.currentUser?.uid

    private val _currentFilter = MutableStateFlow("upcoming")
    private var allVaccines = mutableListOf<Vaccine>()
    private val _filteredVaccines = MutableStateFlow<List<Vaccine>>(emptyList())
    val filteredVaccines: StateFlow<List<Vaccine>> = _filteredVaccines.asStateFlow()
    private var listenerRegistration: ListenerRegistration? = null

    init { listenToVaccines() }

    private fun listenToVaccines() {
        val uid = currentUserUid ?: "demo_user"
        listenerRegistration = db.collection("vaccines")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    allVaccines.clear()
                    for (doc in snapshot.documents) {
                        val vaccine = doc.toObject(Vaccine::class.java)
                        if (vaccine != null) {
                            vaccine.id = doc.id
                            allVaccines.add(vaccine)
                        }
                    }
                    allVaccines.sortBy { it.dueDate }
                    applyFilter(_currentFilter.value)
                }
            }
    }

    fun setFilter(filterType: String) {
        _currentFilter.value = filterType
        applyFilter(filterType)
    }

    private fun applyFilter(filterType: String) {
        val filteredList = if (filterType == "completed") {
            allVaccines.filter { it.isCompleted }
        } else {
            allVaccines.filter { !it.isCompleted }
        }
        _filteredVaccines.value = filteredList
    }

    fun updateVaccineStatus(vaccine: Vaccine, completed: Boolean, onComplete: (Boolean) -> Unit) {
        db.collection("vaccines").document(vaccine.id)
            .update("isCompleted", completed)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun addDummyVaccine(name: String, dueDate: Long) {
        val uid = currentUserUid ?: "demo_user"
        val newVaccine = hashMapOf("userId" to uid, "name" to name, "dueDate" to dueDate, "isCompleted" to false)
        db.collection("vaccines").add(newVaccine)
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}