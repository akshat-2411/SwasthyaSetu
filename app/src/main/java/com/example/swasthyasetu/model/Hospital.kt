package com.example.swasthyasetu.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Hospital(
    val name: String,
    val address: String,
    val rating: Double,
    val lat: Double,
    val lng: Double,
    val phoneNumber: String = "112",
    val specialty: String = "General Hospital & Emergency Care"
) : Parcelable

