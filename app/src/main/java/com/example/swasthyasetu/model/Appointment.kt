package com.example.swasthyasetu.model

data class Appointment(
    val id: String = "",
    val userUid: String = "",
    val hospitalName: String = "",
    val appointmentDate: Long = 0L,
    val timeSlot: String = "",
    val status: String = "Pending",
    val createdAt: Long = 0L
)
