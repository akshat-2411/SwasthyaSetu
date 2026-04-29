package com.example.swasthyasetu.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swasthyasetu.model.TimelineEvent
import com.example.swasthyasetu.repository.HistoryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import kotlin.collections.take

class HistoryViewModel : ViewModel() {

    private val repository = HistoryRepository()
    private val auth = FirebaseAuth.getInstance()
    private val client = OkHttpClient()

    private val _timelineEvents = MutableLiveData<List<TimelineEvent>>()
    val timelineEvents: LiveData<List<TimelineEvent>> = _timelineEvents

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _aiSummary = MutableLiveData<String?>()
    val aiSummary: LiveData<String?> = _aiSummary

    private val _isAiLoading = MutableLiveData<Boolean>()
    val isAiLoading: LiveData<Boolean> = _isAiLoading

    companion object {
        private const val API_KEY = "AIzaSyDwQVr121magYzPhUZW8r17jcoQpp_R41U"
    }

    // -----------------------------
    // 🔹 LOAD HISTORY (UNCHANGED)
    // -----------------------------
    fun loadHistory() {
        val userId = auth.currentUser?.uid ?: "demo_user"
        _isLoading.value = true

        repository.getUnifiedHistory(userId)
            .addOnSuccessListener { unsortedList ->
                _timelineEvents.value = unsortedList.sortedByDescending { it.date }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _timelineEvents.value = emptyList()
                _isLoading.value = false
            }
    }

    // -----------------------------
    // 🔥 GENERATE SUMMARY (UPDATED)
    // -----------------------------
    fun generateHealthSummary(events: List<TimelineEvent>) {
        if (events.isEmpty()) return

        _isAiLoading.value = true

        viewModelScope.launch {
            val result = callGeminiAPI(buildPrompt(events))
            _aiSummary.postValue(result)
            _isAiLoading.postValue(false)
        }
    }

    // -----------------------------
    // 🔹 BUILD PROMPT (CLEAN)
    // -----------------------------
    private fun buildPrompt(events: List<TimelineEvent>): String {

        val recentEvents = events.take(5)

        val builder = StringBuilder("User health history:\n")

        for (event in recentEvents) {
            builder.append(
                "- ${event.type}: ${event.title}\n" +
                        "Details: ${event.description}\n\n"
            )
        }

        builder.append("Give a short 2-line encouraging health summary.")

        return builder.toString()
    }

    // -----------------------------
    // 🔥 COMMON API FUNCTION
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
                    return@withContext "⚠️ AI unavailable"
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
                return@withContext "⚠️ Error: ${e.message}"
            }
        }
    }
}