package com.example.swasthyasetu.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swasthyasetu.model.DiagnosisResult
import com.example.swasthyasetu.util.DiseaseRisk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

class SymptomViewModel : ViewModel() {

    private val _diagnosisResult = MutableLiveData<DiagnosisResult>()
    val diagnosisResult: LiveData<DiagnosisResult> = _diagnosisResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _predictedRisks = MutableLiveData<List<DiseaseRisk>>()
    val predictedRisks: LiveData<List<DiseaseRisk>> = _predictedRisks

    private val _isLoadingAiContext = MutableLiveData(false)
    val isLoadingAiContext: LiveData<Boolean> = _isLoadingAiContext

    private val _aiPredictionContext = MutableLiveData<String>()
    val aiPredictionContext: LiveData<String> = _aiPredictionContext

    companion object {
        private const val API_KEY = "AIzaSyDwQVr121magYzPhUZW8r17jcoQpp_R41U"
    }

    private val client = OkHttpClient()

    // -----------------------------
    // 🔹 Prediction Engine (Same)
    // -----------------------------
    fun computePredictions(selectedSymptoms: List<String>) {
        val risks = com.example.swasthyasetu.util.PredictionEngine.calculateRisk(selectedSymptoms)
        _predictedRisks.postValue(risks)

        if (risks.isNotEmpty()) {
            fetchPredictionAiContext(
                selectedSymptoms,
                risks[0].name,
                risks[0].probability
            )
        }
    }

    // -----------------------------
    // 🔥 AI CONTEXT (UPDATED)
    // -----------------------------
    private fun fetchPredictionAiContext(
        symptoms: List<String>,
        disease: String,
        riskScore: Int
    ) {

        val prompt =
            "User has ${symptoms.joinToString()} with $riskScore% risk of $disease. " +
                    "Give 3 short precautions for rural users."

        _isLoadingAiContext.value = true

        viewModelScope.launch {
            val result = callGeminiAPI(prompt)
            _aiPredictionContext.postValue(result)
            _isLoadingAiContext.postValue(false)
        }
    }

    // -----------------------------
    // 🔥 FALLBACK AI (UPDATED)
    // -----------------------------
    private fun fetchGeminiFallback(selectedList: List<String>) {
        _isLoading.value = true

        val symptomList = selectedList.joinToString(", ")

        val prompt = """
            The user has: $symptomList.
            Suggest illness + urgency + 3 tips.
            Format:
            ILLNESS:
            URGENCY:
            TIP1:
            TIP2:
            TIP3:
        """.trimIndent()

        viewModelScope.launch {
            val response = callGeminiAPI(prompt)
            val result = parseGeminiResponse(response, symptomList)
            _diagnosisResult.postValue(result)
            _isLoading.postValue(false)
        }
    }

    // -----------------------------
    // 🔥 COMMON API CALL (NEW)
    // -----------------------------
    private suspend fun callGeminiAPI(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {

                val json = """
                {
                  "contents": [{
                    "parts":[{"text":"$prompt"}]
                  }]
                }
                """.trimIndent()

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$API_KEY")
                    .post(RequestBody.create("application/json".toMediaType(), json))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (!response.isSuccessful || body.isNullOrEmpty()) {
                    return@withContext "AI unavailable"
                }

                val jsonObject = JSONObject(body)

                return@withContext jsonObject
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

            } catch (e: Exception) {
                return@withContext "Error: ${e.message}"
            }
        }
    }

    // -----------------------------
    // 🔹 YOUR EXISTING LOGIC (UNCHANGED)
    // -----------------------------
    @SuppressLint("NullSafeMutableLiveData")
    fun analyzeSymptoms(selectedList: List<String>) {

        val symptoms = selectedList.map { it.lowercase().trim() }.toSet()

        fun has(vararg names: String): Boolean = names.all { it in symptoms }

        val localResult: DiagnosisResult? = when {

            has("fever", "cough", "body pain", "fatigue") ->
                DiagnosisResult("Severe Viral Flu", listOf("Rest", "Hydrate", "Paracetamol"), "Red")

            else -> null
        }

        if (localResult != null) {
            _diagnosisResult.value = localResult
        } else {
            fetchGeminiFallback(selectedList)
        }
    }

    // -----------------------------
    // 🔹 PARSER (UNCHANGED)
    // -----------------------------
    private fun parseGeminiResponse(rawText: String, symptomList: String): DiagnosisResult {

        val lines = rawText.lines().map { it.trim() }

        val illness = lines.firstOrNull { it.startsWith("ILLNESS:", true) }
            ?.substringAfter(":")?.trim()
            ?: "AI Suggestion"

        val urgency = lines.firstOrNull { it.startsWith("URGENCY:", true) }
            ?.substringAfter(":")?.trim() ?: "Yellow"

        val tips = lines.filter { it.startsWith("TIP", true) }
            .map { it.substringAfter(":").trim() }

        return DiagnosisResult(
            illnessName = illness,
            homeCareTips = if (tips.isNotEmpty()) tips else listOf("Rest", "Hydrate", "Consult doctor"),
            urgencyLevel = urgency,
            isAiGenerated = true
        )
    }
}