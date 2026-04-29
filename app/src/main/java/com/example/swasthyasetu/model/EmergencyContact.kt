package com.example.swasthyasetu.model

data class EmergencyContact(
    val name: String = "",
    val phone: String = "",
    val relationship: String = ""
) {
    constructor() : this("", "", "")
}