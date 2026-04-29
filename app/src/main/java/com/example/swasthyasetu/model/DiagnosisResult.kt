package com.example.swasthyasetu.model

import java.io.Serializable

data class DiagnosisResult(
    val illnessName: String,
    val homeCareTips: List<String>,
    val urgencyLevel: String,   // "Green" | "Yellow" | "Red"
    val isAiGenerated: Boolean = false,
    val disclaimer: String = "This is not a medical diagnosis. Please consult a qualified healthcare professional."
) : Serializable