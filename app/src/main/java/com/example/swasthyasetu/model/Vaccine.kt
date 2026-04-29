package com.example.swasthyasetu.model

data class Vaccine(
    var id: String = "",
    val userId: String = "",
    val name: String = "",
    val dueDate: Long = 0L,
    @field:JvmField var isCompleted: Boolean = false
)
