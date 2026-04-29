package com.example.swasthyasetu.view

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swasthyasetu.databinding.ActivityChatBinding
import com.example.swasthyasetu.model.ChatMessage
import com.example.swasthyasetu.model.UserProfile
import com.example.swasthyasetu.repository.ProfileRepository
import com.example.swasthyasetu.view.BaseActivity
import com.example.swasthyasetu.view.ChatAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter

    private val profileRepository = ProfileRepository()

    // OkHttpClient with proper timeouts for AI API calls
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var systemInstruction: String = BASE_SYSTEM_INSTRUCTION  // default before profile loads
    private var isProfileLoaded = false

    // Conversation history for multi-turn context (stores user + assistant messages)
    private val conversationHistory = mutableListOf<JSONObject>()

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    companion object {
        // ─── GROQ CONFIG ───────────────────────────────────────────────────────────
        val apiKey = BuildConfig.API_KEY
        private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val GROQ_MODEL =
            "llama-3.3-70b-versatile"  // fast & capable; change to "mixtral-8x7b-32768" if preferred

        // ─── SYSTEM INSTRUCTIONS ───────────────────────────────────────────────────
        private const val BASE_SYSTEM_INSTRUCTION =
            """You are a rural health assistant for the Swasthya Setu app. Your job is to provide simple, clear, and helpful health-related advice to users in rural India.

STRICT RULES you must ALWAYS follow:
1. ONLY answer questions related to health, medicine, symptoms, nutrition, hygiene, mental wellness, first aid, or medical advice.
2. If the user asks about ANYTHING unrelated to health (politics, sports, entertainment, coding, etc.), politely refuse and redirect them to ask a health question. Example: "I can only help with health-related questions. Please ask me something about your health or symptoms."
3. Always end medical advice with: "⚠️ Please consult a doctor for proper diagnosis and treatment."
4. Use simple, easy-to-understand language. Avoid complex medical jargon.
5. Be concise — keep answers under 150 words unless the topic genuinely requires more detail.
6. Be empathetic and supportive."""

        private const val HINDI_SUFFIX =
            "\n7. Respond in simple Hindi (Hinglish is acceptable). Keep the disclaimer in Hindi too: \"⚠️ कृपया सही जांच और इलाज के लिए डॉक्टर से मिलें।\""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.inputBar) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.translationY = -imeInsets.bottom.toFloat()
            insets
        }

        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        setupRecyclerView()
        setupInputBar()
        addWelcomeMessage()
        fetchProfileAndBuildInstruction()
        setupMediaLaunchers()
    }

    private fun setupMediaLaunchers() {

        // 📷 Camera
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    handleImageMessage("📷 Photo Captured")
                }
            }
        }

        // 📎 Gallery
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    handleImageMessage("📎 Image Selected")
                }
            }
        }
    }

    // ─── PROFILE & SYSTEM INSTRUCTION ─────────────────────────────────────────

    private fun fetchProfileAndBuildInstruction() {
        lifecycleScope.launch {
            // Wrap with a timeout so a hanging Firestore call doesn't freeze the system instruction
            val profile = try {
                withContext(Dispatchers.IO) {
                    profileRepository.fetchUserProfile()
                }
            } catch (e: Exception) {
                android.util.Log.w("ChatActivity", "Profile fetch failed (non-fatal): ${e.message}")
                null  // chat still works without profile
            }
            systemInstruction = buildSystemInstruction(profile)
            android.util.Log.d(
                "ChatActivity",
                "System instruction ready. Length: ${systemInstruction.length}"
            )
        }
    }

    private fun buildSystemInstruction(profile: UserProfile?): String {
        val builder = StringBuilder(BASE_SYSTEM_INSTRUCTION)

        val currentLang = com.example.swasthyasetu.util.LocaleHelper.getSavedLanguage(this)
        if (currentLang == "hi") {
            builder.append(HINDI_SUFFIX)
        }

        // Append user health profile so the AI can give personalised advice
        if (profile != null) {
            builder.append("\n\nUSER HEALTH PROFILE (use this to personalise your advice):")

            if (!profile.bloodGroup.isNullOrBlank()) builder.append("\n- Blood Group: ${profile.bloodGroup}")
            if (!profile.allergies.isNullOrBlank()) builder.append("\n- Known Allergies: ${profile.allergies}")
        }
        return builder.toString()
    }

    // ─── UI SETUP ──────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupInputBar() {
        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etChatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(); true
            } else false
        }
        binding.btnCamera.setOnClickListener {
            openCamera()
        }

        binding.btnUpload.setOnClickListener {
            openGallery()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private fun handleImageMessage(text: String) {
        chatAdapter.addMessage(ChatMessage(text, true, getCurrentTime()))
        scrollToBottom()

        lifecycleScope.launch {
            val response = getAIResponse(
                "User uploaded an image related to health. Ask them to describe symptoms shown in the image."
            )

            chatAdapter.addMessage(ChatMessage(response, false, getCurrentTime()))
            scrollToBottom()
        }
    }

    private fun addWelcomeMessage() {
        val lang = com.example.swasthyasetu.util.LocaleHelper.getSavedLanguage(this)
        val welcome = if (lang == "hi")
            "👋 नमस्ते! मैं आपका AI स्वास्थ्य सहायक हूँ। कृपया अपनी स्वास्थ्य समस्या बताएं।"
        else
            "👋 Hello! I am your AI health assistant. Please describe your health concern and I'll do my best to help."
        chatAdapter.addMessage(ChatMessage(welcome, false, getCurrentTime()))
    }

    // ─── MESSAGING ────────────────────────────────────────────────────────────

    private fun sendMessage() {
        val userText = binding.etChatInput.text.toString().trim()
        if (userText.isEmpty()) return

        chatAdapter.addMessage(ChatMessage(userText, true, getCurrentTime()))
        // ❌ REMOVE THIS LINE → conversationHistory.add(buildMessageObject("user", userText))
        scrollToBottom()

        binding.etChatInput.text.clear()
        setInputEnabled(false)
        setTypingState(true)

        lifecycleScope.launch {
            val response = getAIResponse(userText)  // ✅ pass userText directly

            conversationHistory.add(buildMessageObject("user", userText))      // ✅ add user msg
            conversationHistory.add(buildMessageObject("assistant", response)) // ✅ add reply
            chatAdapter.addMessage(ChatMessage(response, false, getCurrentTime()))

            scrollToBottom()
            setTypingState(false)
            setInputEnabled(true)
        }
    }

    // ─── GROQ API CALL ────────────────────────────────────────────────────────

    private suspend fun getAIResponse(userText: String): String {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ChatActivity", "Calling Groq API with model: $GROQ_MODEL")

                val messagesArray = JSONArray()
                messagesArray.put(buildMessageObject("system", systemInstruction))
                conversationHistory.forEach { messagesArray.put(it) }
                messagesArray.put(buildMessageObject("user", userText))

                val requestBody = JSONObject().apply {
                    put("model", GROQ_MODEL)
                    put("messages", messagesArray)
                    put("max_tokens", 512)
                    put("temperature", 0.7)
                }.toString()

                android.util.Log.d(
                    "ChatActivity",
                    "Request body: $requestBody"
                ) // ← confirm JSON is valid

                val request = Request.Builder()
                    .url(GROQ_API_URL)
                    .addHeader("Authorization", "Bearer $GROQ_API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                android.util.Log.d("ChatActivity", "Groq response code: ${response.code}")
                android.util.Log.d(
                    "ChatActivity",
                    "Groq response body: $responseBody"
                ) // ← see exact error

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    return@withContext when (response.code) {
                        400 -> "❌ Bad request (400): $responseBody"  // show actual Groq error message
                        401 -> "❌ Invalid API key. Please check your Groq API key."
                        429 -> "⏳ Too many requests. Please wait and try again."
                        500, 503 -> "❌ Groq service temporarily unavailable."
                        else -> "❌ Error (${response.code}): $responseBody"
                    }
                }

                JSONObject(responseBody)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

            } catch (e: java.net.UnknownHostException) {
                "❌ No internet connection."
            } catch (e: java.net.SocketTimeoutException) {
                "❌ Request timed out. Please try again."
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Groq call exception: ${e.message}", e)
                "❌ Error: ${e.message}"
            }
        }
    }
    // ─── HELPERS ──────────────────────────────────────────────────────────────

    /**
     * Builds a JSON message object for the chat messages array.
     * Using JSONObject instead of string interpolation avoids JSON injection
     * when user text contains quotes, newlines, or special characters.
     */
    private fun buildMessageObject(role: String, content: String): JSONObject {
        return JSONObject().apply {
            put("role", role)
            put("content", content)
        }
    }

    private fun setTypingState(isTyping: Boolean) {
        binding.tvTypingIndicator.visibility = if (isTyping) View.VISIBLE else View.GONE
    }

    private fun setInputEnabled(enabled: Boolean) {
        binding.btnSend.isEnabled = enabled
        binding.etChatInput.isEnabled = enabled
    }

    private fun scrollToBottom() {
        if (chatAdapter.itemCount > 0) {
            binding.rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }
}