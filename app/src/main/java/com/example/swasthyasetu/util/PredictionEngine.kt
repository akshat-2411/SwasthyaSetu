package com.example.swasthyasetu.util

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

data class DiseaseRisk(
    val name: String,
    val probability: Int,
    val riskLevel: RiskLevel
)

object PredictionEngine {

    fun calculateRisk(selectedSymptoms: List<String>): List<DiseaseRisk> {
        val symptoms = selectedSymptoms.map { it.lowercase().trim() }

        var fluScore = 0
        var dengueScore = 0

        val hasHighFever = symptoms.any { it.contains("high fever") || it.contains("fever") }
        val hasBodyAche = symptoms.any { it.contains("body ache") || it.contains("body pain") }
        val hasWeakness = symptoms.any { it.contains("weakness") || it.contains("fatigue") }

        if (hasHighFever && hasBodyAche && hasWeakness) {
            fluScore = 70
            dengueScore = 30
        } else {
            if (hasHighFever) { fluScore += 30; dengueScore += 20 }
            if (hasBodyAche) { fluScore += 20; dengueScore += 10 }
            if (hasWeakness) { fluScore += 20 }
        }

        val hasRetroOrbitalPain = symptoms.any { it.contains("retro-orbital pain") || it.contains("eye pain") }
        val hasRash = symptoms.any { it.contains("rash") }

        if (hasRetroOrbitalPain || hasRash) {
            dengueScore += 40
            fluScore -= 30
            if (hasRetroOrbitalPain && hasRash) {
                dengueScore += 20
            }
        }

        var covidScore = 0
        if (symptoms.any { it.contains("loss of taste") || it.contains("loss of smell") || it.contains("cough") }) {
            covidScore += 60
            if (hasHighFever) covidScore += 25
        }

        // Safety: Ensure percentages are capped
        fluScore = fluScore.coerceIn(0, 100)
        dengueScore = dengueScore.coerceIn(0, 100)
        covidScore = covidScore.coerceIn(0, 100)

        val diseases = mutableListOf<DiseaseRisk>()
        if (fluScore > 0) diseases.add(DiseaseRisk("Flu", fluScore, determineRiskLevel(fluScore)))
        if (dengueScore > 0) diseases.add(DiseaseRisk("Dengue", dengueScore, determineRiskLevel(dengueScore)))
        if (covidScore > 0) diseases.add(DiseaseRisk("COVID-19", covidScore, determineRiskLevel(covidScore)))

        return diseases.sortedByDescending { it.probability }
    }

    private fun determineRiskLevel(probability: Int): RiskLevel {
        return when {
            probability >= 70 -> RiskLevel.HIGH
            probability >= 40 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
}

