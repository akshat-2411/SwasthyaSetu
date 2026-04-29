package com.example.swasthyasetu.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

class BookingViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _isSubmitting = MutableLiveData<Boolean>(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _isSuccess = MutableLiveData<Boolean>(false)
    val isSuccess: LiveData<Boolean> = _isSuccess

    private val _appointments = MutableLiveData<List<com.example.swasthyasetu.model.Appointment>>()
    val appointments: LiveData<List<com.example.swasthyasetu.model.Appointment>> = _appointments

    private val _isLoadingAppointments = MutableLiveData<Boolean>(false)
    val isLoadingAppointments: LiveData<Boolean> = _isLoadingAppointments

    /**
     * Poll dynamically scoped bounds securely using Google Authentication rules seamlessly mapping user limits
     */
    fun fetchMyAppointments() {
        val uid = auth.currentUser?.uid ?: "guest_user"
        _isLoadingAppointments.value = true

        db.collection("appointments")
            .whereEqualTo("userUid", uid)
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<com.example.swasthyasetu.model.Appointment>()
                for (document in result) {
                    try {
                        val appointment = document.toObject(com.example.swasthyasetu.model.Appointment::class.java).copy(id = document.id)
                        list.add(appointment)
                    } catch (e: Exception) {
                        // ignore malformed native Firestore bundles gracefully
                    }
                }
                // Sort by descending constraint natively handling newest first cleanly
                list.sortByDescending { it.appointmentDate }
                _appointments.value = list
                _isLoadingAppointments.value = false
            }
            .addOnFailureListener {
                _isLoadingAppointments.value = false
            }
    }

    fun submitBooking(hospitalName: String, dateMills: Long, timeSlot: String) {
        _isSubmitting.value = true

        // Grab strict UUID or fallback cleanly
        val uid = auth.currentUser?.uid ?: "guest_user"

        val appointmentData = hashMapOf(
            "userUid" to uid,
            "hospitalName" to hospitalName,
            "appointmentDate" to dateMills,
            "timeSlot" to timeSlot,
            "status" to "Pending",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("appointments")
            .add(appointmentData)
            .addOnSuccessListener {
                _isSubmitting.value = false
                _isSuccess.value = true
            }
            .addOnFailureListener {
                _isSubmitting.value = false
                _isSuccess.value = false
            }
    }
}