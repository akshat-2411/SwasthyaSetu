package com.example.swasthyasetu.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FirstAidContent(
    val id: String,
    val title: String,
    val description: String,
    val iconResName: String,
    val steps: List<String>
) : Parcelable
