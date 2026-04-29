package com.example.swasthyasetu.model

data class TimelineEvent(
    val id: String,
    val title: String,
    val date: Long,
    val type: String, // "CHAT", "VACCINATION", "SYMPTOM"
    val description: String
)
