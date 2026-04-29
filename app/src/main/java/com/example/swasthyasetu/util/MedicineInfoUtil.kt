package com.example.swasthyasetu.util

object MedicineInfoUtil {

    fun getMedicineInfo(name: String): String {

        val lower = name.lowercase()

        return when {
            lower.contains("paracetamol") ->
                "Used for fever and pain relief."

            lower.contains("ibuprofen") ->
                "Painkiller and anti-inflammatory medicine."

            lower.contains("azithromycin") ->
                "Antibiotic used for infections."

            lower.contains("dolo") ->
                "Dolo 650 is used for fever and body pain."

            else ->
                "Medicine detected. Please consult a doctor before use."
        }
    }
}